package gsn.beans.windowing;

import gsn.beans.StreamElement;
import gsn.beans.StreamSource;

public interface SlidingHandler {
	
	public void addStreamSource(StreamSource streamSource);
	
	public void removeStreamSource(StreamSource streamSource);

	public boolean dataAvailable(StreamElement streamElement);

	public boolean isInterestingIn(StreamSource streamSource);

	public long getOldestTimestamp();
	
	public void finilize();

}