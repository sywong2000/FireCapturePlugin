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
import java.util.Date;
import java.util.Properties;

public class RemoteShutterServerPlugin extends AbstractPlugin implements IFilter {

    private boolean capture = true;
    private IFilterListener captureListener;
    private boolean isCapturing;
    private JButton button;
    private JFrame frame;
    private SocketConnection socketConnectionThread = null;

    private JTextArea logTextArea;
    private JButton buttonRestartSocket;
    public int nFileNameSuffix = 0;

    public boolean isCapturing() {
        return isCapturing;
    }

    public void setIsCapturing(boolean bIsCapturing) {
        isCapturing = bIsCapturing;
    }

    public void addLogText(String sText){
        if (logTextArea != null)
        {
            logTextArea.append(new Date() +"  -  " + sText + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        }
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

            logTextArea = new JTextArea (20,80);
            logTextArea.setEditable(false);
            logTextArea.setLineWrap(true);
            logTextArea.setWrapStyleWord(true);

            buttonRestartSocket = new JButton("Close Window");
            buttonRestartSocket.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    closeConnectWindow();
                }
            });

            JPanel panelBottom = new JPanel();
            panelBottom.add(buttonRestartSocket);
            final JScrollPane scrollPane = new JScrollPane(logTextArea);
            frame.getContentPane().add(scrollPane);
            frame.getContentPane().add(panelBottom);
            frame.setPreferredSize(new Dimension(900, 400));
            frame.setSize(new Dimension(900, 400));
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    closeConnectWindow();
                    e.getWindow().dispose();
                }
            });
        }
        startSocketThread();
        frame.setVisible(true);
        addLogText("Listening on port 8088...");
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
                    Thread.sleep(100);
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
                            addLogText("Starting capture command received...");
                            SwingUtilities.invokeAndWait(() -> captureListener.startCapture());
                            bw.write("1");
                            bw.newLine();
                            bw.flush();
                            break;
                        case 2:
                            addLogText("Increment Suffix command received...");
                            nFileNameSuffix++;
                            bw.write("1");
                            bw.newLine();
                            bw.flush();
                            break;
                        case 3:
                            addLogText("Stop capture command received...");
                            SwingUtilities.invokeAndWait(() -> captureListener.stopCapture());
                            bw.write("1");
                            bw.newLine();
                            bw.flush();
                            break;
//                        case 4:
//                            logTextArea.setText("Emergency snapshot triggered received...");
//                            captureListener.startSnapshot();
//                            bw.write("Emergency snapshot triggered...");
//                            bw.newLine();
//                            bw.flush();
//                            break;
                        case 5:
                            addLogText("Received Termination Request... Exiting...");
                            frame.setVisible(false);
                            SwingUtilities.invokeAndWait(() -> captureListener.stopCapture());
                            run = false;
                            break;
                        case 6:
                            addLogText("Received Query Status Request.. Checking Capture status...");
                            if (isCapturing())
                            {
                                addLogText("Capture is running... Returning Capture status = 1");
                                bw.write("1");
                                bw.newLine();
                                bw.flush();
                            }
                            else
                            {
                                addLogText("Capture is not running... Return Capture status = 0");
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
                    addLogText("Received Command:"+line);
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
        return String.format("_seq_%03d",nFileNameSuffix);
    }

    @Override
    public void appendToLogfile(Properties properties) {

    }
}
