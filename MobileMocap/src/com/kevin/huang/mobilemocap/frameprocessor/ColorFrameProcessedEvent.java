package com.kevin.huang.mobilemocap.frameprocessor;

import java.util.List;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class ColorFrameProcessedEvent {
	public long timeFrameTaken;
	public long time;
	//public Rect noMove;
	//public Rect objectPosition;
	public List<Rect> objectPosition;
	public boolean tracking;
	public List<Point> CentroidList;
	public double LowestMarkerAreas;
	
	public ColorFrameProcessedEvent(long timeFrameTaken, long time, List<Rect> objectPosition, boolean tracking, List<Point> CentroidList, double LowestMarkerAreas) {
		this.timeFrameTaken = timeFrameTaken;
		this.time = time;
		this.objectPosition = objectPosition;
		//this.noMove = noMove;
		this.tracking = tracking;
		this.CentroidList = CentroidList;
		this.LowestMarkerAreas = LowestMarkerAreas;
	}
}
