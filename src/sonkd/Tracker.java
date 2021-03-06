package sonkd;

import java.util.Iterator;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Tracker.java TODO:
 * 
 * @author Kim Dinh Son Email:sonkdbk@gmail.com
 */

public class Tracker extends JTracker {
	int nextTractID = 0;
	public Tracker(float _dt, float _Accel_noise_mag, double _dist_thres,
			int _maximum_allowed_skipped_frames, int _max_trace_length) {
		tracks = new Vector<>();
		dt = _dt;
		Accel_noise_mag = _Accel_noise_mag;
		dist_thres = _dist_thres;
		maximum_allowed_skipped_frames = _maximum_allowed_skipped_frames;
		max_trace_length = _max_trace_length;
	}

	double euclideanDist(Point p, Point q) {
		Point diff = new Point(p.x - q.x, p.y - q.y);
		return Math.sqrt(diff.x * diff.x + diff.y * diff.y);
	}

	public void update(Vector<Rect> rectArray, Mat imag) {
		Vector<Point> detections = new Vector<>();
		detections.clear();
		Iterator<Rect> it3 = rectArray.iterator();
		while (it3.hasNext()) {
			Rect obj = it3.next();
			
			int ObjectCenterX = (int) ((obj.tl().x + obj.br().x) / 2);
			int ObjectCenterY = (int) ((obj.tl().y + obj.br().y) / 2);

			Point pt = new Point(ObjectCenterX, ObjectCenterY);
			detections.add(pt);
			
			Imgproc.rectangle(imag, obj.br(), obj.tl(), new Scalar(
					0, 255, 0), 2);
			Imgproc.circle(imag, pt, 1, new Scalar(0, 0, 255), 2);				
		}
		
		if (tracks.size() == 0) {
			// If no tracks yet
			for (int i = 0; i < detections.size(); i++) {
				Track tr = new Track(detections.get(i), dt,
						Accel_noise_mag, nextTractID++);		
				tracks.add(tr);
			}
		}

		// -----------------------------------
		// Number of tracks and detections
		// -----------------------------------
		int N = tracks.size();
		int M = detections.size();

		// Cost matrix.
		double[][] Cost = new double[N][M]; // size: N, M
		Vector<Integer> assignment = new Vector<>(); // assignment according to Hungarian algorithm
		// -----------------------------------
		// Caculate cost matrix (distances)
		// -----------------------------------
		for (int i = 0; i < tracks.size(); i++) {
			for (int j = 0; j < detections.size(); j++) {
				Cost[i][j] = euclideanDist(tracks.get(i).prediction, detections.get(j));
			}
		}

		// -----------------------------------
		// Solving assignment problem (tracks and predictions of Kalman filter)
		// -----------------------------------
		// HungarianAlg APS = new HungarianAlg();
		// APS.Solve(Cost,assignment, HungarianAlg.TMethod.optimal);

		// HungarianAlg2 APS = new HungarianAlg2();
		// APS.Solve(Cost,assignment);

		AssignmentOptimal APS = new AssignmentOptimal();
		APS.Solve(Cost, assignment);
		// -----------------------------------
		// clean assignment from pairs with large distance
		// -----------------------------------
		// Not assigned tracks
		Vector<Integer> not_assigned_tracks = new Vector<>();

		for (int i = 0; i < assignment.size(); i++) {
			if (assignment.get(i) != -1) {
				if (Cost[i][assignment.get(i)] > dist_thres) {
					assignment.set(i, -1);
					// Mark unassigned tracks, and increment skipped frames
					// counter,
					// when skipped frames counter will be larger than
					// threshold, track will be deleted.
					not_assigned_tracks.add(i);
				}
			} else {
				// If track have no assigned detect, then increment skipped
				// frames counter.			
				tracks.get(i).skipped_frames++;
			}
		}

		// -----------------------------------
		// If track didn't get detects long time, remove it.
		// -----------------------------------
		for (int i = 0; i < tracks.size(); i++) {
			if (tracks.get(i).skipped_frames > maximum_allowed_skipped_frames) {				
				tracks.remove(i);
				assignment.remove(i);
				i--;
			}
		}
		
		// -----------------------------------
		// Search for unassigned detects
		// -----------------------------------
		Vector<Integer> not_assigned_detections = new Vector<>();
		for (int i = 0; i < detections.size(); i++) {
			if (!assignment.contains(i)) {
				not_assigned_detections.add(i);
			}
		}

		// -----------------------------------
		// and start new tracks for them.
		// -----------------------------------
		if (not_assigned_detections.size() > 0) {
			for (int i = 0; i < not_assigned_detections.size(); i++) {
				Track tr = new Track(detections.get(not_assigned_detections.get(i)), dt,
						Accel_noise_mag, nextTractID++);
				tracks.add(tr);
			}
		}
		
		// Update Kalman Filters state

		for (int i = 0; i < assignment.size(); i++) {
			// If track updated less than one time, than filter state is not
			// correct.

			tracks.get(i).KF.getPrediction();

			if (assignment.get(i) != -1) // If we have assigned detect, then
											// update using its coordinates,
			{
				tracks.get(i).skipped_frames = 0;
				tracks.get(i).prediction = tracks.get(i).KF.update(
						detections.get(assignment.get(i)), true);
			} else // if not continue using predictions
			{
				tracks.get(i).prediction = tracks.get(i).KF.update(new Point(0,
						0), false);
			}

			if (tracks.get(i).trace.size() > max_trace_length) {
				// tracks.get(i).trace.erase(tracks.get(i).trace.begin(),tracks.get(i).trace.end()-max_trace_length);
				for (int j = 0; j < tracks.get(i).trace.size()
						- max_trace_length; j++)
					tracks.get(i).trace.remove(j);
			}		
			
			tracks.get(i).trace.add(tracks.get(i).prediction);
			tracks.get(i).KF.setLastResult(tracks.get(i).prediction);
		}
		
		for (int j = 0; j < assignment.size(); j++) {
			if (assignment.get(j) != -1) {
				Point pt2 = new Point(
						(int) ((rectArray.get(assignment.get(j)).tl().x + rectArray
								.get(assignment.get(j)).br().x) / 2), rectArray
								.get(assignment.get(j)).tl().y);
				Imgproc.putText(imag, tracks.get(j).track_id + "", pt2,
						2 * Core.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255,
								255), 1);
			}
		}
	}

}
