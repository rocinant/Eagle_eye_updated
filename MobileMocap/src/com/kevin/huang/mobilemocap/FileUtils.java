package com.kevin.huang.mobilemocap;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

public class FileUtils {
	
	private static final String TAG = "FileSystem";
	private String SDPATH;
	//final long currentTimeMillis;
	final String currentTime;
	final String appName;
	final String DocumentPath;
	final String FilePath;
	final String txtFilePath;
	public File txtFile=null;
	Context context;

	public String getSDPATH() {
		return SDPATH;
	}
	public FileUtils(Context context) throws IOException {
		//得到当前外部存储设备的目录
		// /SDCARD
		this.context= context;
		SDPATH = Environment.getExternalStorageDirectory() + "/"; //.getAbsolutePath() 
		//Public files
		//SDPATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/";
		//private storage directory on the external storage by calling, pass null to receive the root directory of your app's private directory on the external storage
		//When the user uninstalls your app, the system removes your app's files from here only if you save them in the directory from getExternalFilesDir().
		//SDPATH = context.getExternalFilesDir(null)+ "/"; 
		//SDPATH = System.getenv("SECONDARY_STORAGE");
		//currentTimeMillis = System.currentTimeMillis();
		currentTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		appName = context.getString(R.string.app_name);
		//DocumentPath =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
		DocumentPath =SDPATH;
		FilePath = DocumentPath + "/" + appName;
		//txtFilePath = FilePath + "/" + currentTimeMillis + ".txt";
		txtFilePath = FilePath + "/" + currentTime + ".txt";
		// Ensure that the file directory exists.
		File File = new File(FilePath);
		if (!File.isDirectory() && !File.mkdirs()) {			
			Log.e(TAG, "Failed to create file directory at " + FilePath);
			return;
			}
		//txtFile = new File(FilePath, currentTimeMillis + ".txt");
		txtFile = new File(txtFilePath);
		txtFile.createNewFile();
		new File(FilePath + java.io.File.separator + ".nomedia").createNewFile();
	}
	
	public File getTxtFile () {
		return txtFile;
	}
	
	public String getTxtFilePath () {
		return txtFilePath;
	}
	
	public void write2SDFromFloatFileWriter(float buffer []){
		
		if (judgeIfSafe2Write (this.getTxtFilePath ())) {
		//OutputStream output = null;
		FileOutputStream fos = null;
		FileWriter writer= null;
		BufferedWriter bw = null;
		try{
			
			fos = new FileOutputStream(this.getTxtFile());
			writer = new FileWriter(this.getTxtFile ()); //new FileOutputStream(this.getTxtFile());
			bw = new BufferedWriter(writer);
			
			// for each byte in the buffer
	         for (float f: buffer)
	         {
	            // write float to the dos
	        	 //writer.write(String.format("%f", f));
	        	 bw.write(String.format("%f ", f));//输出字符串
	         }
			
/*			for(int i=0 ; i< buffer.length ; i++) {
				output.writeFloat(buffer[i]);
			}*/
	      // force bytes to the underlying stream
	         bw.newLine();
             bw.flush(); 
/*	         writer.write("\n");
	         writer.flush();*/
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			try{
				if(bw!=null) bw.close();
				if(writer!=null) writer.close();
				if(fos!=null) fos.close();
				MediaScannerConnection.scanFile(this.context, new String[] { txtFile.getAbsolutePath() }, null, null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	  }
	}
	public void write2SDFromFloatPrintWriter(float buffer []){
		
		if (judgeIfSafe2Write (this.getTxtFilePath ())) {
		//OutputStream output = null;
		FileOutputStream fos = null;
		PrintWriter writer = null;
		try{
			
			fos = new FileOutputStream(this.getTxtFile());
			writer = new PrintWriter(fos); //new FileOutputStream(this.getTxtFile());
			
			// for each byte in the buffer
	         for (float f: buffer)
	         {
	            // write float to the dos
	        	 writer.print(f);       
	         }
			
/*			for(int i=0 ; i< buffer.length ; i++) {
				output.writeFloat(buffer[i]);
			}*/
	      // force bytes to the underlying stream
	         writer.println();
	         writer.flush();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			try{
				if(writer!=null) writer.close();
				if(fos!=null) fos.close();
				MediaScannerConnection.scanFile(this.context, new String[] { txtFile.getAbsolutePath() }, null, null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	  }
	}

	public void write2SDFromFloatPrintStream(float buffer []){
		
		if (judgeIfSafe2Write (this.getTxtFilePath ())) {
		//OutputStream output = null;
		FileOutputStream fos = null;
		PrintStream printStream = null;
		try{
			
			fos = new FileOutputStream(this.getTxtFile());
			printStream = new PrintStream(fos); //new FileOutputStream(this.getTxtFile());
			
			// for each byte in the buffer
	         for (float f: buffer)
	         {
	            // write float to the dos
	        	 printStream.print(f);       
	         }
			
/*			for(int i=0 ; i< buffer.length ; i++) {
				output.writeFloat(buffer[i]);
			}*/
	      // force bytes to the underlying stream
	        printStream.println();
	        printStream.flush();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			try{
				if(printStream!=null) printStream.close();
				if(fos!=null) fos.close();
				MediaScannerConnection.scanFile(this.context, new String[] { txtFile.getAbsolutePath() }, null, null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	  }
	}
	
	public void write2SDFromFloatDataOutputStream(float buffer []){
		
		if (judgeIfSafe2Write (this.getTxtFilePath ())) {
		//OutputStream output = null;
		FileOutputStream fos = null;
		DataOutputStream output = null;
		try{
			
			fos = new FileOutputStream(this.getTxtFile());
			output = new DataOutputStream(fos); //new FileOutputStream(this.getTxtFile());
			
			// for each byte in the buffer
	         for (float f: buffer)
	         {
	            // write float to the dos
	        	 output.writeFloat(f);         
	         }
			
/*			for(int i=0 ; i< buffer.length ; i++) {
				output.writeFloat(buffer[i]);
			}*/
	      // force bytes to the underlying stream
			output.flush();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			try{
				if(output!=null) output.close();
				if(fos!=null) fos.close();
				MediaScannerConnection.scanFile(this.context, new String[] { txtFile.getAbsolutePath() }, null, null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	  }
	}
	
	public void write2SDFromFloatFast(float buffer []){
		
		if (judgeIfSafe2Write (this.getTxtFilePath ())) {
/*			Log.e(TAG , "writing begin");
			Log.e(TAG , Environment.getExternalStorageState());
			Log.e(TAG , Environment.MEDIA_MOUNTED);*/
		RandomAccessFile aFile =null;
		FileChannel outChannel = null;
		try{
			
			aFile = new RandomAccessFile(this.getTxtFilePath (), "rw");
	        outChannel = aFile.getChannel();

	        //one float 4 bytes
	        ByteBuffer buf = ByteBuffer.allocate(4*buffer.length);
	        buf.clear();
	        buf.asFloatBuffer().put(buffer);

	        //while(buf.hasRemaining()) 
	        {
	            outChannel.write(buf);
	        }

	        //outChannel.close();
	        buf.rewind();

/*	        float[] out=new float[3];
	        buf.asFloatBuffer().get(out);*/

	        outChannel.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			try{
				if (aFile!=null) aFile.close();
				MediaScannerConnection.scanFile(this.context, new String[] { txtFile.getAbsolutePath() }, null, null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		}
	}

	public boolean judgeIfSafe2Write (String Filename) {
		return ( !(isFileExist(Filename)) && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ) ; //&& Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()
	}
	/**
	 * 在SD卡上创建文件
	 * 
	 * @throws IOException
	 */
	public File creatSDFile(String fileName) throws IOException {
		File file = new File(SDPATH + fileName);
		file.createNewFile();
		return file;
	}
	
	/**
	 * 在SD卡上创建目录
	 * 
	 * @param dirName
	 */
	public File creatSDDir(String dirName) {
		File dir = new File(SDPATH + dirName);
		dir.mkdirs();
		return dir;
	}

	/**
	 * 判断SD卡上的文件夹是否存在
	 */
	public boolean isFileExist(String fileName){
		File file = new File(SDPATH + fileName);
		return file.exists();
	}
	
	/**
	 * 将一个InputStream里面的数据写入到SD卡中
	 */
	public File write2SDFromInput(String path,String fileName,InputStream input){
		File file = null;
		OutputStream output = null;
		try{
			creatSDDir(path);
			file = creatSDFile(path + fileName);
			output = new FileOutputStream(file);
			byte buffer [] = new byte[4 * 1024];
			while((input.read(buffer)) != -1){
				output.write(buffer);
			}
			output.flush();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			try{
				output.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		return file;
	}

}