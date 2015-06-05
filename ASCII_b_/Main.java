import static org.imgscalr.Scalr.resize;


//import java.awt.Color;
//import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javax.swing.*;

import java.awt.*;

import org.imgscalr.Scalr;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

/**
 * Finds the source images posted on 4chan.org/b/'s first page.
 * Reads this data using a ruby script.
 * Downloads them, converts them to ASCII art, and saves them to 
 * a file located in outputs/ascii_out_MM-dd-yyyy_HH.mm.ss.txt.
 * @author Reed
 *
 */
public class Main {
	/**
	 * The BufferedImages received during program operation.
	 */
	public ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
	/**
	 * The main frame for displaying images.
	 */
	private JFrame frame;
	/**
	 * Holds the largest dimensions of an image
	 */
	private ArrayList<int[]> dimensions = new ArrayList<int[]>();
	/**
	 * The index of the image currently being shown
	 */
	private int pos = 0;
	/**
	 * 
	 */
	private Component currentImage = null;

	public static void main(String[] args) {
		try {
			new Main().execute(1, false); //Change the scale here
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(new Frame(), "Problem with Ruby script.");
			System.exit(1);
		}
	}

	/**
	 * Performs the functions of the program
	 * @param scale - Double ranging from 0.1 to 1, percentage to scale source images by
	 * @throws IOException - If an error occurs reading or writing to the file
	 */
	private void execute(double scale, boolean generateASCII) throws IOException {
		Process ruby = Runtime.getRuntime().exec("ruby test4ch.rb"); //Runs ruby script
		//Runtime.getRuntime().exec("java -Xms64m -Xmx256m jdbc_prog"); //Increase heap size
		
		//Wait for script to finish polling 4chan.
		try {
			ruby.waitFor();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		BufferedReader br = new BufferedReader(new FileReader("images.txt")); //Open file
		String line;
		ArrayList<String> toPrint= new ArrayList<String>(); //Storing outputs
		if (scale != 1) {
			toPrint.add("Resize factor: " + scale);
		}

		final JProgressBar progress = new JProgressBar();
		progress.setStringPainted(true);

		//Creating progress bar
		JFrame progressBarFrame = new JFrame("Loading Images");
		JPanel p = new JPanel();
		progressBarFrame.setContentPane(p);
		p.add(progress);
		progressBarFrame.pack();
		progressBarFrame.setVisible(true);

		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				progress.setIndeterminate(false);
			}
		});

		int count = 0, total = 0;

		/* Get total number of images*/
		BufferedReader br2 = new BufferedReader(new FileReader("images.txt")); //Open file
		while ((br2.readLine()) != null) {
			total++;
		}
		br2.close();

		/* URL reading block */
		while ((line = br.readLine()) != null) {
			String line2 = line;
			line2 = line2.substring(line2.indexOf('i'));
			line2 = line2.substring(0, line2.length() - 1);
			line2 = "http://" + line2; //Further format URL
			line2 = line2.replace("s", "");

			BufferedImage b = null;
			try {
				b = toBufferedImage(ImageIO.read(new URL(line2))); //Receieve and save images
				if (scale != 1) {
					b = resize(b, Scalr.Mode.AUTOMATIC,
							(int)(b.getWidth() * scale), 
							(int)(b.getHeight() * scale));
				}

				if (b != null) {
					images.add(b); //Store the image
					dimensions.add(new int[]{b.getWidth(), b.getHeight()}); //Store its height and width
				}

				/* Percentage for progress bar*/
				count++;
				if (count > total) {
					count = total;
				}
				
				final int percent = count * 100 / total;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						progress.setString(percent + "%");
						progress.setValue(percent);
					}	
				});

			} catch (FileNotFoundException e2) {//Image 404s
				System.err.println("Image " + line2 + " 404'd!");

			} catch (IIOException e) {//Cannot connect to internet
				System.out.println("Skip");
				count++;
				//e.printStackTrace();
				//				JOptionPane.showMessageDialog(new Frame(), "Failed to connect to Internet.");
				//				System.exit(1);

			} 

			System.out.println("Read " + line2);
			toPrint.add("\n" + line2 + "\n\n\n"); //Add URLS to output file container


			/*ASCII block */
			if (generateASCII) { 
				ASCII i = new ASCII();
				String ascii = i.convert(b); //Convert BufferedImage into ASCII art
				toPrint.add(ascii); //Add to output container

				br.close();
				DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy_HH.mm.ss");

				String fileName = "ascii_out_" + dateFormat.format(new Date()); //Format date string
				BufferedWriter bw = new BufferedWriter(new FileWriter("outputs/" + fileName + ".txt")); //Create file name
				Iterator<String> a = toPrint.iterator();

				while (a.hasNext()) { //Write ASCII to file
					String temp = a.next();

					bw.write(temp);
				}
				bw.close();
			}
		}
		createUI();
	}

	//Returns a bufferedImage from image, from some github page
	private static BufferedImage toBufferedImage(Image img)
	{
		if (img instanceof BufferedImage)
		{
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}

	/**
	 * Creates the base frame and buttons to cycle through images.
	 */
	private void createUI() {
		if (frame != null) {
			frame.dispose();
		}

		frame = new JFrame("Images");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final JPanel main = new JPanel();
		frame.setContentPane(main);
		JButton next = new JButton("Next");
		JButton prev = new JButton("Previous");
		JButton refresh = new JButton("Refresh");

		JPanel topPanel = new JPanel();
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(prev, BorderLayout.WEST);
		bottomPanel.add(Box.createHorizontalStrut(15));
		bottomPanel.add(next);

		topPanel.add(refresh);

		main.setLayout(new BorderLayout());
		main.add(bottomPanel, BorderLayout.NORTH);
		main.add(topPanel, BorderLayout.SOUTH);

		ImageIcon curr = convert(images.get(pos)); //The first image to be shown
		displayImage(curr); //Add first iamge

		/* * * * * * * * * * * * * * * * *
		 * * * * * Button Actions* * * * *
		 * * * * * * * * * * * * * * * * */

		//Next
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				next();
			}
		});

		//Final settings
		frame.pack();
		frame.setVisible(true);

		prev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				prev();
			}			
		});

		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					execute(1, false);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}	
		});
	}

	/**
	 * Converts the given BufferedImage into an ImageIcon with appropriate scale.
	 * Scales the image down to within 1440x900px.
	 * @param b BufferedImage to resize and convert
	 * @return A resized ImageIcon of the image.
	 */
	private ImageIcon convert(BufferedImage b) {
		ImageIcon toReturn = null;
		if (b.getWidth() > 1440) {
			if (b.getHeight() > 900) { //Width too large, height too large
				BufferedImage b2 = resize(b, Scalr.Mode.FIT_TO_HEIGHT, 1440, 900);
				toReturn = new ImageIcon(b2);
			} else { //Width too large, height okay
				BufferedImage b2 = resize(b, Scalr.Mode.FIT_TO_WIDTH, 1440, 900);
				toReturn = new ImageIcon(b2);
			}
		} else {
			if (b.getHeight() > 900) { //Width okay, height too big
				BufferedImage b2 = resize(b, Scalr.Mode.FIT_TO_HEIGHT, 1440, 900);
				toReturn = new ImageIcon(b2);
			} else {//Both okay
				toReturn = new ImageIcon(b);
			}
		}

		return toReturn;
	}
	/**
	 * Action for the Next button
	 */
	private final void next() {
		ImageIcon toDisplay = convert(forward());
		frame.getContentPane().remove(currentImage);
		displayImage(toDisplay);

		frame.revalidate();
		frame.pack(); //Resize window
	}

	/**
	 * Action for the Previous button
	 */
	private final void prev() {
		ImageIcon toDisplay = convert(backwards());
		frame.getContentPane().remove(currentImage);
		displayImage(toDisplay);

		frame.revalidate();
		frame.pack(); //Resize window
	}

	/**
	 * Adds the given ImageIcon to the given panel.
	 * @param i - Image to be displayed
	 * @param main - Frame for image to be linked to
	 */
	private void displayImage(ImageIcon i) {
		JLabel l = new JLabel("", i, JLabel.CENTER);
		currentImage = frame.getContentPane().add(l); //Returns added image
		System.out.println("Displaying "+ "pos: " + pos + " w: " +
				i.getIconWidth() + ", h: " + i.getIconHeight());
	}

	/**
	 * Move forward one image in the array.
	 * @return
	 */
	private BufferedImage forward() {
		if (++pos <= images.size() - 1) {
			return images.get(pos);
		} else {
			return images.get(pos = 0);
		}
	}

	/**
	 * Move backwards one image in the array, or loop around to the end.
	 * @return
	 */
	private BufferedImage backwards() {
		if (pos >= 1) { //Move backwards
			return images.get(--pos);
		} else { //Pos == 0
			return images.get(pos = images.size() - 1);
		}
	}

	//	private static void writeToImage(String s) {
	//		BufferedImage p = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	//		Graphics2D g2d = p.createGraphics();
	//
	//		Font f = new Font("Lucida Console", Font.PLAIN, 10);
	//		g2d.setFont(f);
	//		FontMetrics fm = g2d.getFontMetrics();
	//		int width = fm.stringWidth(s);
	//		int height = fm.getHeight() * s.length() - s.replace("\n", "").length(); //Count lines in string
	//		g2d.dispose();
	//		
	//		p = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	//		g2d = p.createGraphics();
	//		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
	//		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
	//		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
	//        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
	//        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
	//        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	//        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
	//        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	//
	//        g2d.setFont(f);
	//        g2d.setColor(Color.BLACK);
	//        g2d.drawString(s, 0, fm.getAscent());
	//        g2d.dispose();
	//        
	//        try {
	//			ImageIO.write(p, "png", new File("test.png"));
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//	}
}
