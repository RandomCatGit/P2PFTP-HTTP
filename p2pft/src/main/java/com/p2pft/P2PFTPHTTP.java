package com.p2pft;

import static spark.Spark.get;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

/**
 * P2PFTPHTTP is a simple tool for sending files through P2P connections over HTTP. It mimics FTP but does not have the
 * access/directory control. Select a file or a folder and send the generated URL to the recipient to start download.
 * Folders are zipped for single entity processing and to keep things simple.
 *
 * @author RandomCatGit
 */
public class P2PFTPHTTP {
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		JFrame mainFrame = new JFrame("P2P FTP/HTTP");
		mainFrame.setResizable(false);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainFrame.setLayout(null);
		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		int width = 610, height = 200;
		mainFrame.setBounds((int) screenDim.getWidth() / 2 - width / 2, (int) screenDim.getHeight() / 2 - height / 2,
				width, height);

		int rowCount = 0;
		int compHeight = 25;
		int compSpace = 35;

		JLabel source = new JLabel("Select source file/folder");
		source.setBounds(20, compSpace * rowCount++ + 10, 500, compHeight);
		mainFrame.add(source);

		JTextField sourcePath = new JTextField();
		sourcePath.setBounds(20, compSpace * rowCount++ + 10, 500, compHeight);
		sourcePath.setEditable(false);
		mainFrame.add(sourcePath);

		JButton folderEx = new JButton("...");
		folderEx.setBounds(521, (int) sourcePath.getBounds().getY() - 1, 60, compHeight + 2);
		mainFrame.add(folderEx);

		JLabel host = new JLabel("Fetch URL");
		host.setBounds(20, compSpace * rowCount++ + 10, 500, compHeight);
		mainFrame.add(host);

		JTextField hostPath = new JTextField();
		hostPath.setBounds(20, compSpace * rowCount++ + 10, 500, compHeight);
		hostPath.setEditable(false);
		mainFrame.add(hostPath);

		JButton hostCopy = new JButton("Copy");
		hostCopy.setBounds(521, (int) hostPath.getBounds().getY() - 1, 60, compHeight + 2);
		hostCopy.setEnabled(false);
		mainFrame.add(hostCopy);

		folderEx.addActionListener((e) -> {
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setMultiSelectionEnabled(false);
			if (fc.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
				sourcePath.setText(fc.getSelectedFile().getAbsolutePath());
				hostPath.setText(startHost(fc.getSelectedFile().getAbsolutePath()));
				hostCopy.setEnabled(true);
			}
		});

		hostCopy.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(hostPath.getText()),
						null);
			}
		});

		mainFrame.setVisible(true);
	}

	/**
	 * startHost method is used for
	 * 
	 * @param absolutePath
	 * @return
	 */
	private static String startHost(String absolutePath) {
		File downFile = new File(absolutePath);
		String downPath = "/" + UUID.randomUUID().toString();
		get(downPath, (req, res) -> {
			res.type("application/octect-stream");
			String fileName;
			FileInputStream fileInputStream;
			if (downFile.isDirectory()) { // is a dir
				final File tempZipFile = new File(downFile.getName() + ".zip");
				tempZipFile.delete();
				ZipFile zipFile = new ZipFile(tempZipFile.getAbsoluteFile());
				ZipParameters parameters = new ZipParameters();

				parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
				parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

				zipFile.addFolder(downFile, parameters);
				fileName = zipFile.getFile().getName();
				fileInputStream = new FileInputStream(zipFile.getFile());
			} else { // is a file
				fileName = downFile.getName();
				fileInputStream = new FileInputStream(downFile);
			}
			res.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

			PrintWriter out = res.raw().getWriter();
			int i;
			while ((i = fileInputStream.read()) != -1) {
				out.write(i);
			}
			fileInputStream.close();
			out.close();
			return "File " + fileName + " is being downloaded.";
		});
		try {
			return InetAddress.getLocalHost().getHostAddress() + ":4567" + downPath;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return "Host exception";
	}
}
