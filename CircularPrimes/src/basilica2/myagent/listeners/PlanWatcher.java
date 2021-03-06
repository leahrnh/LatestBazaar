package basilica2.myagent.listeners;

import java.util.ArrayList;
import java.util.HashMap;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.ReadyEvent;
import basilica2.agents.events.WhiteboardEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PrioritySource;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.myagent.events.PlanEvent;
import basilica2.social.events.DormantGroupEvent;
import basilica2.socketchat.WebsocketChatClient;
import basilica2.tutor.events.DoTutoringEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

public class PlanWatcher implements BasilicaPreProcessor
{
    public PlanWatcher() 
    {
    	myConcepts.put("RECORDER_API",0);
    	myConcepts.put("AUDIO_SOURCE",0);
    	myConcepts.put("AUDIO_ENCODER",0);
    	myConcepts.put("OUTPUT_FORMAT",0);
    	myConcepts.put("PREPARE",0);
    	myConcepts.put("START",0);
    	myConcepts.put("STOP",0);
    	myConcepts.put("RELEASE",0);
    	NumConcepts = 8;
    	plan="";
	}
    
    private Integer NumConcepts;
    
    private String plan;
    
	private HashMap<String, Integer> myConcepts = new HashMap<String, Integer>();
    
    public void textToGraphics(String s)
    {
        String text = s;

        /*
           Because font metrics is based on a graphics context, we need to create
           a small, temporary image so we can ascertain the width and height
           of the final image
         */
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        Font font = new Font("Arial", Font.PLAIN, 48);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        g2d.dispose();

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, 0, fm.getAscent());
        g2d.dispose();
        try {
            ImageIO.write(img, "png", new File("Text.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
	/**
	 * @param source the InputCoordinator - to push new events to. (Modified events don't need to be re-pushed).
	 * @param event an incoming event which matches one of this preprocessor's advertised classes (see getPreprocessorEventClasses)
	 * 
	 * Preprocess an incoming event, by modifying this event or creating a new event in response. 
	 * All original and new events will be passed by the InputCoordinator to the second-stage Reactors ("BasilicaListener" instances).
	 */
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		if (event instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent)event;
			String[] annotations = me.getAllAnnotations();
			
			for (String s: annotations)
		    {
				if(myConcepts.containsKey(s) && myConcepts.get(s)==0)
				{
					myConcepts.put(s,1);
					NumConcepts = NumConcepts - 1;
					plan = plan + s + "\n";
					
					textToGraphics(plan);
					String working_dir = System.getProperty("user.dir");
					working_dir = working_dir + "/Text.png";
					
					System.out.println("Directory : " + working_dir);
					try {
						working_dir = new File(working_dir).toURI().toURL().toString();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("Directory url : " + working_dir);
					
					WhiteboardEvent w = new WhiteboardEvent(source,working_dir , false);
					//WhiteboardEvent w = WhiteboardEvent.makeWhiteboardMessage("audio","audio", source);
					source.pushProposal(new PriorityEvent(source, w, 
							1.0, new PrioritySource("Whiteboard", false), 60.0));
				}

				if(NumConcepts < 1)
				{
					//this new event will be added to the queue for second-stage processing.
					PlanEvent plan = new PlanEvent(source,  "Okay, it seems plan is ready now. You're on your own from here on. Good luck!", "NOTICE_DONE");
					source.queueNewEvent(plan);							
				}
				
				if(s.equals("HELP"))
				{           
					for (String key : myConcepts.keySet()) {
					    if(myConcepts.get(key)==0)
					    {					
					    	DoTutoringEvent toot = new DoTutoringEvent(source, key);
							source.addPreprocessedEvent(toot);
							myConcepts.put(key, 1);
							NumConcepts = NumConcepts - 1;
							break;
					    }
					}
				}			
	        }								    
	    }
		else if (event instanceof ReadyEvent)
		{
			if(NumConcepts < 1)
			{
				
				PlanEvent plan = new PlanEvent(source,  "Okay, it seems plan is ready now. You're on your own from here on. Good luck!", "NOTICE_DONE");
				
				source.queueNewEvent(plan);
			
			}
			else
			{
				for (String key : myConcepts.keySet()) {
				    if(myConcepts.get(key)==0)
				    {					
						
				    	PlanEvent plan = new PlanEvent(source,  "Plan is not yet complete. You are missing some steps. Let me help you with this.", "INCOMPLETE");

						source.queueNewEvent(plan);
					    
				    	DoTutoringEvent toot = new DoTutoringEvent(source, key);
						source.addPreprocessedEvent(toot);
				    	
						myConcepts.put(key, 1);
						NumConcepts = NumConcepts - 1;
						break;
				    }
				}
			}						
		}
		/*else if (event instanceof DormantGroupEvent)
		{
			for (String key : myConcepts.keySet()) {
			    if(myConcepts.get(key)==0)
			    {					
					DoTutoringEvent toot = new DoTutoringEvent(source, key);
					source.addPreprocessedEvent(toot);
					myConcepts.put(key, 1);
					NumConcepts = NumConcepts - 1;
					break;
			    }
			}
		}	*/			
	}

	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		//only MessageEvents will be delivered to this watcher.
		return new Class[]{MessageEvent.class, ReadyEvent.class, DormantGroupEvent.class};
	}
}
