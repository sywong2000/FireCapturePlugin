package org.sywong2000.FCPlugin;

import de.wonderplanets.firecapture.plugin.AbstractPlugin;
import de.wonderplanets.firecapture.plugin.CamInfo;
import de.wonderplanets.firecapture.plugin.IFilter;
import de.wonderplanets.firecapture.plugin.IFilterListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class RemoteShutterServerPlugin extends AbstractPlugin implements IFilter {

    private boolean capture = true;
    private IFilterListener captureListener;
    private boolean isCapturing;
    private JButton button;
    private JFrame frame;
    private SocketConnection socketConnectionThread = null;

    private JLabel statusLabel;
    private JButton buttonRestartSocket;

    public boolean isCapturing() {
        return isCapturing;
    }

    public void setIsCapturing(boolean bIsCapturing) {
        isCapturing = bIsCapturing;
    }




    @Override
    public String getName() {
        return "Remote Shutter Server Plugin";
    }

    @Override
    public String getDescription() {
        return "Start/stop capture from an external app";
    }

    @Override
    public String getMaxValueLabel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCurrentValueLabel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getStringUsage(int percent) {
        return "Press button";
    }

    @Override
    public boolean useValueFields() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean useSlider() {
        return false;
    }

    @Override
    public String getMaxValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCurrentValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void sliderValueChanged(int value) {
        // TODO Auto-generated method stub
    }

    @Override
    public int getInitialSliderValue() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void imageSizeChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void filterChanged(String prevFilter, String filter) {
        // TODO Auto-generated method stub

    }

    @Override
    public void activated() {

    }

    @Override
    public void release() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean capture() {
        return capture;
    }

    @Override
    public void computeMono(byte[] bytePixels, Rectangle imageSize, CamInfo info) {
        // TODO Auto-generated method stub
    }

    @Override
    public void computeColor(int[] rgbPixels, Rectangle imageSize, CamInfo info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void captureStoped() {
        // TODO Auto-generated method stub
        setIsCapturing(false);
    }

    @Override
    public void captureStarted() {
        // TODO Auto-generated method stub
        setIsCapturing(true);
    }

    @Override
    public boolean isNullFilter() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean processEarly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsColor() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean supportsMono() {
        return true;
    }

    @Override
    public void registerFilterListener(IFilterListener listener) {
        captureListener = listener;
    }

    @Override
    public JButton getButton() {
        if (button == null) {
            button = new JButton("Connect");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openConnectWindow();
                }
            });
        }
        return button;
    }

    protected void openConnectWindow() {
        if (frame == null) {

            frame = new JFrame();
            frame.setLayout(new FlowLayout());

            statusLabel = new JLabel("Waiting for command...");
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));
            statusLabel.setForeground(Color.red);
            statusLabel.setBorder(BorderFactory.createTitledBorder("Status"));

            buttonRestartSocket = new JButton("Close Window");
            buttonRestartSocket.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    closeConnectWindow();
                }
            });

            JPanel panelTop = new JPanel();
            panelTop.setPreferredSize(new Dimension(390, 300));
            statusLabel.setPreferredSize(new Dimension(390, 300));
            panelTop.add(statusLabel);
            JPanel panelBottom = new JPanel();
            panelBottom.add(buttonRestartSocket);
            frame.getContentPane().add(panelTop);
            frame.getContentPane().add(panelBottom);
            frame.setPreferredSize(new Dimension(400, 400));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
        startSocketThread();
        frame.setVisible(true);
    }

    protected void closeConnectWindow() {

        stopSocketThread();
        if (frame != null) {
            frame.setVisible(false);
            frame = null;
        }
    }

    private void startSocketThread() {
        try {
            if (socketConnectionThread == null) {
                socketConnectionThread = new SocketConnection(8088);
            }
            if (!socketConnectionThread.isAlive()) {
                socketConnectionThread.start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void stopSocketThread()
    {
        if (socketConnectionThread != null)
        {
            while (!socketConnectionThread.server.isClosed()) {
                socketConnectionThread.stopConnection();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            socketConnectionThread = null;
        }
    }

    class SocketConnection extends Thread {

        private final ServerSocket server;
        public boolean run = true;

        public SocketConnection(int port) throws IOException {
            this.server = new ServerSocket(port);
        }

        public void stopConnection() {
            this.run = false;
            try {
                if (server != null) {
                    server.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            Socket socket = null;
            try {
                socket = server.accept();
                BufferedReader bwin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                while (run) {
                    switch (readInput(bwin.readLine())) {

                        case 1:
                            statusLabel.setText("Starting capture command received...");
                            SwingUtilities.invokeAndWait(() -> captureListener.startCapture());
                            bw.write("1");
                            bw.newLine();
                            bw.flush();
                            break;
//                        case 2:
//                            statusLabel.setText("Toggle capture command received...");
//                            capture = !capture;
//                            bw.write(capture ? "Capture resumed..." : "Capture paused...");
//                            bw.newLine();
//                            bw.flush();
//                            break;
                        case 3:
                            statusLabel.setText("Stop capture command received...");
                            SwingUtilities.invokeAndWait(() -> captureListener.stopCapture());
                            bw.write("1");
                            bw.newLine();
                            bw.flush();
                            break;
//                        case 4:
//                            statusLabel.setText("Emergency snapshot triggered received...");
//                            captureListener.startSnapshot();
//                            bw.write("Emergency snapshot triggered...");
//                            bw.newLine();
//                            bw.flush();
//                            break;
                        case 5:
                            frame.setVisible(false);
                            SwingUtilities.invokeAndWait(() -> captureListener.stopCapture());
                            run = false;
                            break;
                        case 6:
                            if (isCapturing())
                            {
                                statusLabel.setText("Return Capture status = 1");
                                bw.write("1");
                                bw.newLine();
                                bw.flush();
                            }
                            else
                            {
                                statusLabel.setText("Return Capture status = 0");
                                bw.write("0");
                                bw.newLine();
                                bw.flush();
                            }
                            break;
                        default:
                            break;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (socket != null)
                    try {
                        socket.close();

                    } catch (IOException e) {

                    }
                if (server != null) {
                    try {
                        server.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }

        private int readInput(String line) {
            if (line == null) {
                return -1;
            } else {
                try {
                    statusLabel.setText("Received Command:"+line);
                    return Integer.parseInt(line);
                } catch (Exception e) {
                    return -1;
                }
            }
        }

    }

    @Override
    public String getInterfaceVersion() {
        return "1.1";
    }

    @Override
    public String getFilenameAppendix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void appendToLogfile(Properties properties) {

    }
}
