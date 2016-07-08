package com.kevin.huang.mobilemocap.frameprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.hardware.Camera;
import android.util.Log;

import com.kevin.huang.mobilemocap.frameprocessor.Configuration;;

public class ColorFrameProcessor extends FrameProcessor {
	
	private static final String TAG = "ColorPreviewProcessing";
	private long timeStamp = 0;
	private IColorFrameProcessedListener mListener;
	private boolean mIsColorSelected = false;
	//average color pixel selected
	//private Scalar mBlobColorRgba;
	private Scalar mBlobColorHsv;
	private ColorBlobDetector mDetector;
	
	
/*	private Mat mSpectrum;
	
	private static Size SPECTRUM_SIZE;*/
	
	private Mat mRgba, mYuv;
	static public Point TouchedPixel;
	
	public ColorFrameProcessor(IColorFrameProcessedListener listener) {
		this.mListener = listener;
	}
	
	@Override
	public void ready() {
		// TODO Auto-generated method stub
		
/*		SPECTRUM_SIZE = new Size(200, 32);
		mSpectrum = new Mat();*/
		
		//mBlobColorRgba = new Scalar(255);
		mBlobColorHsv = new Scalar(255);
		//fit with the buffer size
		mYuv = new Mat(this.getFrameSize().height + this.getFrameSize().height / 2, this.getFrameSize().width, CvType.CV_8UC1);     	
    	mRgba = new Mat();
    	mDetector = new ColorBlobDetector();
    	//can change the color radius here
    	mDetector.setColorRadius(new Scalar(25,45,45,0));
	}
	
	private class ColorBlobDetector
	{
		// Lower and Upper bounds for range checking in HSV color space
		private Scalar mLowerBound = new Scalar(0);
		private Scalar mUpperBound = new Scalar(0);
		// Color radius for range checking in HSV color space
		private Scalar mColorRadius = new Scalar(25,50,50,0);
		
		//useless code
		//private Mat mSpectrum = new Mat();
		
		private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
		
		private List<Point> centroidList = new ArrayList<Point>();

		private double lowestMarkerAreas;
			
		public void setColorRadius(Scalar radius)
		{
			mColorRadius = radius;
		}
		
		//set the target color
		public void setHsvColor(Scalar hsvColor)
		{
		    double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0; 
	    	double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

	  		mLowerBound.val[0] = minH;
	   		mUpperBound.val[0] = maxH;

	  		mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
	   		mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

	  		mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
	   		mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

	   		mLowerBound.val[3] = 0;
	   		mUpperBound.val[3] = 255;
	   		
	   		// target color's spectrum, seems only contain selected color's hue information, useless code
	   		
/*	   		Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);
	 		for (int j = 0; j < maxH-minH; j++)
	   		{
	   			byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
	   			spectrumHsv.put(0, j, tmp);
	   		}
	   		Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);*/

		}
		
		// get target color's spectrum, useless code
/*		public Mat getSpectrum()
		{
			return mSpectrum;
		}*/
		
		private class ArrayIndexComparator implements Comparator<Integer>
		{
		    private final double[] array;

		    public ArrayIndexComparator(double[] array)
		    {
		        this.array = array;
		    }

		    public Integer[] createIndexArray()
		    {
		        Integer[] indexes = new Integer[array.length];
		        for (int i = 0; i < array.length; i++)
		        {
		            indexes[i] = i; // Autoboxing
		        }
		        return indexes;
		    }

		    @Override
		    public int compare(Integer index1, Integer index2)
		    {
		         // Autounbox from Integer to int to use as array indexes
		        //return array[index1].compareTo(array[index2]);
		    	return Double.compare(array[index1], array[index2]);
		    }
		}
		
		//find the target's maximum area contour, input target color is RGB format
		public void process(Mat rgbaImage)
		{
	    	Mat pyrDownMat = new Mat();

	    	Imgproc.pyrDown(rgbaImage, pyrDownMat);
	    	Imgproc.pyrDown(pyrDownMat, pyrDownMat);

	      	Mat hsvMat = new Mat();
	    	Imgproc.cvtColor(pyrDownMat, hsvMat, Imgproc.COLOR_RGB2HSV_FULL);
	    	//form binary plot and dilate it
	    	Mat Mask = new Mat();
	    	Core.inRange(hsvMat, mLowerBound, mUpperBound, Mask);
	    	Mat dilatedMask = new Mat();
	    	Imgproc.dilate(Mask, dilatedMask, new Mat());

	        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	        Mat hierarchy = new Mat();
	        //Find contour arrayList in MatOfPoint form
	        //should try to use different method here to look into detail efficiency
	        Imgproc.findContours(dilatedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
	        
	        mContours.clear();
	        centroidList.clear();
	        
	        /*for (int i=0; i < indexes.length; i++){
	        	Log.e(TAG, "" + indexes[i]);
	        	Log.e(TAG, "" + contourAreas [indexes[i]]);
	        }*/
	        
	        //This part we sort the area and keep tracking the indices
	        double [] contourAreas = new double [contours.size()];
	        for (int i=0; i < contours.size(); i++){
	        	MatOfPoint wrapper = contours.get(i);
	        	contourAreas [i] = Imgproc.contourArea(wrapper);
        	}
	        ArrayIndexComparator comparator = new ArrayIndexComparator(contourAreas);
	        Integer[] indexes = comparator.createIndexArray();
	        Arrays.sort(indexes, comparator);
	        // Now the indexes are in appropriate order.
		    //this part we change to multiple image targets location
	        int targetNumber = contours.size();
	        if (contours.size() > Configuration.TARGET_NUMBER) targetNumber = Configuration.TARGET_NUMBER;
	        
	        double maximumY = 0;
	        	
	        for (int i=0; i < targetNumber; i++){
	        	
	        	MatOfPoint contour = contours.get(indexes[indexes.length - 1 - i]);
	        	
	        	Moments moments = Imgproc.moments(contour);
        		Point centroid = new Point();
        		centroid.x = moments.get_m10() / moments.get_m00();
        		centroid.y = moments.get_m01() / moments.get_m00();
        		if (centroid.y > maximumY) {
        			maximumY = centroid.y;
        			lowestMarkerAreas = Imgproc.contourArea(contour);
        		} 
        		centroidList.add(centroid);
        		
        		Core.multiply(contour, new Scalar(4,4), contour);
        		mContours.add(contour);
        	}
	        
	        /*//find more than desired targets so need to filter to defined number of targets using area
	        if (contours.size() > Configuration.TARGET_NUMBER) {
	        	 //This part we sort the area and keep tracking the indices
		        double [] contourAreas = new double [contours.size()];
		        for (int i=0; i < contours.size(); i++){
		        	MatOfPoint wrapper = contours.get(i);
		        	contourAreas [i] = Imgproc.contourArea(wrapper);
	        	}
		        ArrayIndexComparator comparator = new ArrayIndexComparator(contourAreas);
		        Integer[] indexes = comparator.createIndexArray();
		        Arrays.sort(indexes, comparator);
		        // Now the indexes are in appropriate order.
			    //this part we change to multiple image targets location
		        
		        for (int i=0; i < Configuration.TARGET_NUMBER; i++){
		        	
		        	MatOfPoint contour = contours.get(indexes[indexes.length - 1 - i]);
		        	
		        	Moments moments = Imgproc.moments(contour);
	        		Point centroid = new Point();
	        		centroid.x = moments.get_m10() / moments.get_m00();
	        		centroid.y = moments.get_m01() / moments.get_m00();
	        		centroidList.add(centroid);
	        		
	        		Core.multiply(contour, new Scalar(4,4), contour);
	        		mContours.add(contour);
	        	}
	        }
	        else {
	        	//This part we sort the area and keep tracking the indices
		        double [] contourAreas = new double [contours.size()];
		        for (int i=0; i < contours.size(); i++){
		        	MatOfPoint wrapper = contours.get(i);
		        	contourAreas [i] = Imgproc.contourArea(wrapper);
	        	}
		        ArrayIndexComparator comparator = new ArrayIndexComparator(contourAreas);
		        Integer[] indexes = comparator.createIndexArray();
		        Arrays.sort(indexes, comparator);
		        
		        for (int i=0; i < indexes.length; i++){
		        	
		        	MatOfPoint contour = contours.get(indexes[indexes.length - 1 - i]);
		        	
		        	Moments moments = Imgproc.moments(contour);
	        		Point centroid = new Point();
	        		centroid.x = moments.get_m10() / moments.get_m00();
	        		centroid.y = moments.get_m01() / moments.get_m00();
	        		centroidList.add(centroid);
	        		
	        		Core.multiply(contour, new Scalar(4,4), contour);
	        		mContours.add(contour);
	        	}
		        // Now the indexes are in appropriate order.
		        //this part we change to multiple image targets location
		        Iterator<MatOfPoint> each = contours.iterator();
		        while (each.hasNext())
		        {
		        	MatOfPoint contour = each.next();
		        	
		        	Moments moments = Imgproc.moments(contour);
	        		Point centroid = new Point();
	        		centroid.x = moments.get_m10() / moments.get_m00();
	        		centroid.y = moments.get_m01() / moments.get_m00();
	        		centroidList.add(centroid);
	        		
	        		Core.multiply(contour, new Scalar(4,4), contour);
	        		mContours.add(contour);
		        	
		        }
		        
	        }*/
	        
	        /*//Here need to change to find multiple approximate area contours
	        // Find max contour area
	        double maxArea = 0;
	        //Iterator<MatOfPoint> maximumAreaPosition=null;
	        Iterator<MatOfPoint> each = contours.iterator();
	        while (each.hasNext())
	        {
	        	MatOfPoint wrapper = each.next();
	        	double area = Imgproc.contourArea(wrapper);
	        	if (area > maxArea)
	        	{
	        		maxArea = area;
	        		//maximumAreaPosition=each;
	        	}
	        }

	        mContours.clear();
	        centroidList.clear();
    		
    		// Filter contours by area and resize to fit the original image size
	        each = contours.iterator();
	        while (each.hasNext())
	        {
	        	MatOfPoint contour = each.next();

	        	if (Imgproc.contourArea(contour) == maxArea)
	        	{
	        		Moments moments = Imgproc.moments(contour);

	        		Point centroid = new Point();

	        		centroid.x = moments.get_m10() / moments.get_m00();
	        		centroid.y = moments.get_m01() / moments.get_m00();
	        		centroidList.add(centroid);
	        		
	        		//why is  Scalar(4,4) to recover the down sample
	        		Core.multiply(contour, new Scalar(4,4), contour);
	        		mContours.add(contour);
	        	}
	        }*/
	        
	       /* //this part we change to multiple image targets location
	        mContours.clear();
	        centroidList.clear();
	        Iterator<MatOfPoint> each = contours.iterator();
	        while (each.hasNext())
	        {
	        	MatOfPoint contour = each.next();
	        	
	        	Moments moments = Imgproc.moments(contour);

        		Point centroid = new Point();

        		centroid.x = moments.get_m10() / moments.get_m00();
        		centroid.y = moments.get_m01() / moments.get_m00();
        		centroidList.add(centroid);
        		Core.multiply(contour, new Scalar(4,4), contour);
        		mContours.add(contour);
	        	
	        }*/
	        
	        
		}

		public List<MatOfPoint> getContours()
		{
			return mContours;
		}
		
		public List<Point> getCentroidList()
		{
			return centroidList;
		}
		
		public double getLowestMarkerAreas()
		{
			return lowestMarkerAreas;
		}
		
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		
		long timeStampFrameTaken = System.currentTimeMillis();
		
		mYuv.put(0, 0, data);
	    Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4);
	    
	    //Rect objectPosition = null;
	    List<Rect> objectPosition = new ArrayList<Rect>();
	    List<Point> centroidList = new ArrayList<Point>();
	    //Point centroid=null;
	    
	    //should not be createded every camera frame
	    //Rect noMove = null;
	    
	    if (mIsColorSelected)
        {            
        	mDetector.process(mRgba);
        	List<MatOfPoint> contours = mDetector.getContours();
        	//useless code delete to fast speed
            //Log.e(TAG, "Contours count: " + contours.size());
            
            if(contours.size() > 0) {
            	//here to add the coordinator calculation and saving it to SD card text file
            	
            	objectPosition.clear();
            	centroidList.clear();
            	
            	/*for (int i=0; i < contours.size(); i++){
            	objectPosition.add(Imgproc.boundingRect(contours.get(i)));
            	}*/
            	
            	Iterator<MatOfPoint> each = contours.iterator();
     	        while (each.hasNext()) {
     	        	MatOfPoint contour = each.next();
     	        	objectPosition.add(Imgproc.boundingRect(contour));
     	        }
            	
            	centroidList = mDetector.getCentroidList();
            	
            	//Log.e(TAG, "centroid x: " + centroidList.get(0).x + "centroid y: " + centroidList.get(0).y);
            	
            	//useless code, actually has nothing to do with objectPosition and should not be conducted every camera frame
            	//noMove = getNoMoveRect(objectPosition);
            }
        }
	    
		long newTimeStamp = System.currentTimeMillis();
		//data processed, add buffer back
		mCamera.addCallbackBuffer(data);
		//or here to add the coordinator calculation and saving it to SD card text file reference Core.sumElems
		//noMove should not be sent every camera frame
		mListener.onFrameProcessed(new ColorFrameProcessedEvent(timeStampFrameTaken, newTimeStamp - timeStamp, objectPosition, mIsColorSelected, centroidList, mDetector.getLowestMarkerAreas()));
		timeStamp = newTimeStamp;
	}
	
	//Actually has nothing to do with rect
	public Rect getNoMoveRect()
	{
		int width = (int)Math.round(Configuration.NO_MOVE_RECT_RATIO * this.getFrameSize().width);
		int height = (int)Math.round(Configuration.NO_MOVE_RECT_RATIO * this.getFrameSize().height);
		int x = (this.getFrameSize().width / 2) - (width / 2);
		int y = (this.getFrameSize().height / 2) - (height / 2);
		//actually should be Math.min(width, this.getFrameSize().width - 2*x - 1); need to leave space for the right and bottom screen
		width = Math.min(width, this.getFrameSize().width - x - 1);
		height = Math.min(height, this.getFrameSize().height - y - 1);
		x = Math.max(x, 0);
		y = Math.max(y, 0);
			
		return new Rect(x, y, width, height);
	}
	
	public boolean onTouch(int x, int y) {
		//useless code delete to fast speed
		 Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")"); 
		 
        if ((x < 0) || (y < 0) || (x > this.mFrameSize.width) || (y > this.mFrameSize.height)) return false;
  
        TouchedPixel = new Point (x,y);
        Rect touchedRect = new Rect();
        
        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < this.getFrameSize().width) ? x + 4 - touchedRect.x : this.mFrameSize.width - touchedRect.x;
        touchedRect.height = (y+4 < this.getFrameSize().height) ? y + 4 - touchedRect.y : this.mFrameSize.height - touchedRect.y;
        	
        Mat touchedRegionRgba = mRgba.submat(touchedRect);
        
        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
        
        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
        {
        	mBlobColorHsv.val[i] /= pointCount;
        }
        
        //useless code
/*        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);
        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] + 
    			", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");*/
   		
   		mDetector.setHsvColor(mBlobColorHsv);
   		
   	    // target color's spectrum, seems only contain selected color's hue information () transform to RGB, useless code
   		//Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
   		
        mIsColorSelected = true;
        return false; // don't need subsequent touch events
	}
	
	private Scalar converScalarHsv2Rgba(Scalar hsvColor)
	{	
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
       
        return new Scalar(pointMatRgba.get(0, 0));
	}

}
