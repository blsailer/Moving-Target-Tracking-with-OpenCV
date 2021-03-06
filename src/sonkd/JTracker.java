package sonkd;

import java.util.Vector;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * JTracker.java TODO:
 * 
 * @author Kim Dinh Son Email:sonkdbk@gmail.com
 */

public abstract class JTracker {

	public float dt;

	public float Accel_noise_mag;

	public double dist_thres;

	public int maximum_allowed_skipped_frames;

	public int max_trace_length;

	public Vector<Track> tracks;

	public abstract void update(Vector<Rect> detections, Mat imag);

}
