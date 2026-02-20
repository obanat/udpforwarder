package com.obana.vpnforwarder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import com.obana.vpnforwarder.utils.AppLog;

public class UdpFilterVpnService extends VpnService {
    private static final String TAG = "UdpFilterVpnService";
    private static final String CHANNEL_ID = "UdpVpnChannel";
    private static final int NOTIFICATION_ID = 1; // 通知ID，必须大于0
    private ParcelFileDescriptor vpnInterface;
    private volatile boolean isRunning = false;
    private static final String SP_KEY_MAC = "mac";

    private static final String REDIS_HOST= "i4free.x3322.net";
    private static final int REDIS_PORT = 38086;

    // 目标过滤配置
    private static final byte[] TARGET_IP = {(byte) 192, (byte) 168, 2, 1}; // 192.168.2.1 mavic pro
    private static final String TARGET_IP_STR = "192.168.2.1"; //udp port of mavic pro
    private static final int TARGET_PORT = 9003; //udp port of mavic pro
    // 转发目标配置
    private static final int LOCAL_UDP_SOURCE_PORT = 10002; //djigo4 use 10003 as udp sender port
    private static final String LOCAL_VPS_IP = "10.144.0.2"; //
    private static final String LOCAL_VPS_NAME = "UDPFilterVpn"; //
    private static final String FORWARD_IPV4_TEST = "192.168.5.5"; // 测试用途，需替换为实际的IPv6地址
    private static final int FORWARD_PORT = 34000;

    private FileOutputStream vpnOutput;
    private FileInputStream vpnInput;

    private final byte[] sourceIp = {(byte)192,(byte)168, 5, (byte)161};
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            startForegroundService();
            new Thread(this::runVpn).start();
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        // 创建通知渠道 (Android 8.0+ 必须)
        createNotificationChannel();

        // 点击通知时打开的Activity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // 构建通知
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("UDP转发器运行中")
                .setContentText("正在监听并转发流量...")
                .setSmallIcon(R.drawable.app) // 请确保有此图标资源
                .setContentIntent(pendingIntent)
                .build();

        // 关键API：将服务提升为前台服务
        // 参数1：通知ID，参数2：通知对象
        startForeground(NOTIFICATION_ID, notification);
    }


    private void runVpn() {
        // 1. 配置VPN接口
        Builder builder = new Builder();
        builder.setSession(LOCAL_VPS_NAME);
        builder.addAddress(LOCAL_VPS_IP, 32); // 虚拟内部IP
        builder.addRoute("0.0.0.0", 0);     // 拦截所有IPv4流量
        try {
            vpnInterface = builder.establish();
            vpnInput = new FileInputStream(vpnInterface.getFileDescriptor());
            vpnOutput = new FileOutputStream(vpnInterface.getFileDescriptor());
            // 分配缓冲区，MTU通常为1500或更大
            ByteBuffer buffer = ByteBuffer.allocate(32767);
            // 2. 建立转发Socket (IPv6)
            // 注意：必须protect socket，否则会形成环路
            DatagramSocket forwardSocket = new DatagramSocket();
            protect(forwardSocket);
            startResponseListener(forwardSocket);//start output thread


            String redisIpv6 = getIpv6HostName();
            //InetAddress forwardAddress = Inet6Address.getByName(redisIpv6);
            InetAddress forwardAddress = Inet4Address.getByName(FORWARD_IPV4_TEST);
            AppLog.i(TAG,"runVpn forwardAddress:"+forwardAddress);

            while (isRunning) {
                // 3. 读取数据包
                int length = vpnInput.read(buffer.array());
                if (length > 0) {
                    byte[] rawData = buffer.array();
                    // 4. 解析IP头 (简易解析，假设无IP选项)
                    // IP版本号 (高4位)
                    int version = (rawData[0] >> 4) & 0x0F;
                    // 仅处理IPv4数据包
                    if (version == 4) {
                        // IP头长度 (IHL, 低4位，单位为4字节)
                        int ipHeaderLength = (rawData[0] & 0x0F) * 4;
                        // 协议字段 (偏移量9)
                        int protocol = rawData[9] & 0xFF;
                        // 目标IP (偏移量16-19)
                        boolean ipMatch = (rawData[16] == TARGET_IP[0] &&
                                rawData[17] == TARGET_IP[1] &&
                                rawData[18] == TARGET_IP[2] &&
                                rawData[19] == TARGET_IP[3]);

                        //AppLog.i(TAG,"receive 1 ip,TARGET_IP:"+rawData[16] + "." + rawData[17] + "."
                                //+ rawData[18] + "." + rawData[19]);

                        // 5. 筛选逻辑：UDP协议 且 目标IP匹配
                        // Protocol 17 = UDP
                        if (protocol == 17 && ipMatch) {
                            //sourceIp[0] = rawData[12];sourceIp[1] = rawData[13];
                            //sourceIp[2] = rawData[14];sourceIp[3] = rawData[15];
                            AppLog.i(TAG,"match 1 udp package, sourceIp:"+sourceIp[0] + "." + sourceIp[1] + "."
                                    + sourceIp[2] + "." + sourceIp[3]);
                            // 提取UDP数据
                            // UDP头占8字节，Data从 IP头长 + 8 开始
                            // 这里我们将整个IP包的有效载荷或原始数据转发
                            // 根据需求，通常转发去掉IP头的UDP载荷
                            // 获取源端口和目的端口（可选逻辑）
                            // int destPort = ((rawData[ipHeaderLength + 2] & 0xFF) << 8) | (rawData[ipHeaderLength + 3] & 0xFF);
                            // 提取UDP载荷 (去掉IP头和UDP头)
                            int udpHeaderLength = 8;
                            int payloadLength = length - ipHeaderLength - udpHeaderLength;
                            if (payloadLength > 0) {
                                byte[] payload = new byte[payloadLength];
                                System.arraycopy(rawData, ipHeaderLength + udpHeaderLength, payload, 0, payloadLength);
                                // 6. 转发至IPv6目标
                                DatagramPacket sendPacket = new DatagramPacket(
                                        payload,
                                        payload.length,
                                        forwardAddress,
                                        FORWARD_PORT
                                );
                                forwardSocket.send(sendPacket);
                                AppLog.i(TAG,"send 1 udp package, addr:"+forwardAddress + " port:" + FORWARD_PORT
                                        + " len:" + payload.length);
                            }
                        }
                    }
                }
                buffer.clear();
            }
        } catch (Exception e) {
            AppLog.e(TAG, "udp send error:" + e.getMessage());
        } finally {
            if (vpnInterface != null) {
                try { vpnInterface.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }
    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }


    /*
    获取
    * */
    private String getIpv6HostName() {

        String url ;
        String clientId = getSharedPreference(SP_KEY_MAC, "dji0001");
        String redisServerIp = "i4free.x3322.net";
        String redisServerPort = "38086";

        url = String.format("http://%s:%d/wificar/getClientIp?mac=%s", REDIS_HOST,REDIS_PORT,clientId);

        AppLog.i(TAG, "wificar server url:" + url);

        String ipaddr = getURLContent(url);

        AppLog.i(TAG, "ip v6 addr:" + ipaddr);
        return ipaddr;
    }

    private static String getURLContent(String url) {
        StringBuffer sb = new StringBuffer();

        try {
            URL updateURL = new URL(url);
            URLConnection conn = updateURL.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF8"));
            while (true) {
                String s = rd.readLine();
                if (s == null) {
                    break;
                }
                sb.append(s);
            }
        } catch (Exception e){

        }
        return sb.toString();
    }

    private String getSharedPreference(String key, String def) {
        //return AndRovio.getSharedPreference(key);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(key, def);
    }

    private void startResponseListener(DatagramSocket socket) {
        int originalSourcePort = LOCAL_UDP_SOURCE_PORT;//udp port of djigo4
        new Thread(() -> {
            try {
                byte[] buffer = new byte[32767];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                while (!socket.isClosed()) {
                    // 1. 接收服务器响应
                    socket.receive(responsePacket);
                    int dataLen = responsePacket.getLength();
                    AppLog.i(TAG,"recv 1 udp package, addr:"+sourceIp[0]+"."+sourceIp[1]
                            + " port:" + responsePacket.getPort()
                            + " len:" + dataLen);
                    // 2. 构造IP头 + UDP头 + 数据
                    // 总长度 = 20(IP头) + 8(UDP头) + 数据长度
                    int totalLength = 20 + 8 + dataLen;
                    byte[] fakeIpPacket = new byte[totalLength];
                    // --- 构造IPv4头 (20字节) ---
                    fakeIpPacket[0] = (byte) 0x45; // Version(4) + IHL(5)
                    // Total Length (2字节)
                    fakeIpPacket[2] = (byte) (totalLength >> 8);
                    fakeIpPacket[3] = (byte) totalLength;
                    // ID, Flags, Fragment Offset (通常设为0即可)
                    fakeIpPacket[8] = (byte) 64; // TTL
                    fakeIpPacket[9] = (byte) 17; // Protocol: UDP (17)
                    // Source IP: 192.168.2.1 (伪造源地址，欺骗应用)
                    fakeIpPacket[12] = (byte) 192;
                    fakeIpPacket[13] = (byte) 168;
                    fakeIpPacket[14] = (byte) 2;
                    fakeIpPacket[15] = (byte) 1;
                    // Destination IP: 10.0.0.2 (手机虚拟IP)
                    // 注意：这里建议解析原始包记录目的IP，简单场景可直接使用配置

                    //byte[] localIpBytes = InetAddress.getByName(TARGET_IP_STR).getAddress();
                    //byte[] localIpBytes = TARGET_IP;
                    //替换IP头:source ip,把4g模块的ip为本机ip,用于欺骗
                    System.arraycopy(sourceIp, 0, fakeIpPacket, 16, 4);
                    // IP Checksum (必须计算，否则系统会丢弃)
                    int checksum = calculateChecksum(fakeIpPacket, 20);
                    fakeIpPacket[10] = (byte) (checksum >> 8);
                    fakeIpPacket[11] = (byte) checksum;
                    // --- 构造UDP头 (8字节) ---
                    // Source Port: 通常是服务器端口，但这里应用期望的是它发送时目的端口
                    // UDP头在IP头后，偏移量20
                    int udpStart = 20;
                    // 原始请求的目的端口(应用视角的源端口) -> 现在作为响应的源端口?
                    // 不对，应用发起时：Src=AppPort, Dst=TargetPort
                    // 收到响应时：Src=TargetPort, Dst=AppPort
                    // 但因为我们过滤了所有发往192.168.2.1的包，应用可能连接的是不同端口。
                    // 这里简化处理：假设应用连接的是标准端口，或者我们需要在Map中记录原始目的端口。
                    // 为了简化，假设原始目的端口已知(如53)，或者我们在map中存了更多信息。
                    // 这里我们假设原始应用连接的是任意端口，我们需要在handleOutgoingPacket中记录原始目的端口。
                    // 此处仅演示：将响应数据写回。
                    // Source Port: (伪造) 原始应用请求的目的端口
                    // Dest Port: originalSourcePort (发回给原始应用)
                    // 假设我们需要回写的目的端口就是 originalSourcePort
                    fakeIpPacket[udpStart + 0] = (byte) (TARGET_PORT >> 8); // Dst Port High
                    fakeIpPacket[udpStart + 1] = (byte) TARGET_PORT;        // Dst Port Low
                    fakeIpPacket[udpStart + 2] = (byte) (originalSourcePort >> 8); // Dst Port High
                    fakeIpPacket[udpStart + 3] = (byte) originalSourcePort;        // Dst Port Low
                    // Length (UDP头+数据)
                    int udpLen = 8 + dataLen;
                    fakeIpPacket[udpStart + 4] = (byte) (udpLen >> 8);
                    fakeIpPacket[udpStart + 5] = (byte) udpLen;
                    // UDP Checksum (IPv4 UDP可为0，系统会处理或忽略)
                    fakeIpPacket[udpStart + 6] = 0;
                    fakeIpPacket[udpStart + 7] = 0;
                    // --- 拷贝数据 ---
                    System.arraycopy(responsePacket.getData(), responsePacket.getOffset(), fakeIpPacket, 28, dataLen);
                    // 3. 写入VPN接口，发回给应用
                    if (vpnOutput != null) {
                        vpnOutput.write(fakeIpPacket);
                    }
                }
            } catch (Exception e) {
                AppLog.e(TAG,"recv&send error:" + e.getMessage());
            }
        }).start();
    }
    // 简单的IP头校验和计算
    private int calculateChecksum(byte[] buf, int len) {
        int sum = 0;
        for (int i = 0; i < len; i += 2) {
            int b1 = (buf[i] & 0xFF) << 8;
            int b2 = (buf[i + 1] & 0xFF);
            sum += (b1 + b2);
        }
        // 处理溢出
        while ((sum >> 16) > 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        return ~sum;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "UDP转发服务",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}