import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeReader;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String filename = "scan2.png";
		int crop_x, crop_y, crop_width, crop_height;
		crop_x = 0;
		crop_y = 0;
		crop_width = 1200;
		crop_height = 1200;

		float[][] default_qr = new float[][]{
				{877,196},
				{877,121},
				{952,122},
				{940,184},
			};
		
		float[][] default_box1 = new float[][]{ // clockwise from top left
				{84, 70},
				{620, 70},
				{620, 525},
				{84, 525}
			};
		
		float[][] default_box2 = new float[][]{ // clockwise from top left
				{644, 575},
				{1190, 575},
				{1190, 1060},
				{644, 1060}
			};
		
		
		try {
			// Make bufferedImage, BufferedImageLuminanceSource (cropped) and BinaryBitmap
			BufferedImage bufImg = ImageIO.read(new FileInputStream(filename));
			BufferedImageLuminanceSource bils = new BufferedImageLuminanceSource(
					bufImg, crop_x, crop_y, crop_width, crop_height);
			BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
					bils));

			// Read the QR code
			Result result = new QRCodeReader().decode(binaryBitmap);
			System.out.println(result.getText());
			ResultPoint[] pts = result.getResultPoints();
			
			// What is the size of the detected QR code? 
			int qr_unit = (int) (pts[0].getY() - pts[1].getY());
			System.out.println("QR unit size: " + qr_unit);
			
			// Location of QR code
			for (ResultPoint pt : pts){
				int x = (int) (pt.getX() + crop_x);
				int y = (int) (pt.getY() + crop_y);
				System.out.println(x + "," + y);
			}
			
			/*
			// try some affine transform stuff
			System.out.println("wheres the half coming from? "  + pts[1].getX() + "," + crop_x +", " + default_qr[1][0]);
			double tx = pts[1].getX() + crop_x - default_qr[1][0];
			double ty = pts[1].getY() + crop_y - default_qr[1][1];
			double sx = 1;
			double sy = 1;
			double r = 0.02;
			double a = Math.cos(r) * sx;
			double b = -1 * Math.sin(r) * sx;
			double c = Math.sin(r) * sy;
			double d = Math.cos(r) * sy;
			double e = Math.cos(r) * tx + Math.sin(r) * ty;
			double f = -1 * Math.sin(r) * tx + Math.cos(r) * ty;
			AffineTransform xform = new AffineTransform(a,b,c,d,e,f);
			
			System.out.println("tx, ty: " + tx + "," + ty);
			System.out.println("sx, sy: " + sx + "," + sy);
			System.out.println("rot: " + r);
			System.out.println("affine: " + xform);
			*/
			
			AffineTransform orig = new AffineTransform();
			orig.setTransform(default_qr[2][0] - default_qr[1][0], default_qr[2][1] - default_qr[1][1], 
					default_qr[0][0] - default_qr[1][0], default_qr[0][1] - default_qr[1][1],
					default_qr[1][0], default_qr[1][1]);
			
			AffineTransform scan = new AffineTransform();
			scan.setTransform(pts[2].getX() - pts[1].getX(), pts[2].getY() - pts[1].getY(), 
					pts[0].getX() - pts[1].getX(), pts[0].getY() - pts[1].getY(),
					pts[1].getX() + crop_x, pts[1].getY() + crop_y);
		
			AffineTransform orig_to_scan = new AffineTransform();
			orig_to_scan.setToIdentity();
			orig_to_scan.concatenate(scan);
			orig.invert();
			orig_to_scan.concatenate(orig);
			
			System.out.println("orig " + orig);
			System.out.println("scan " + scan);
			System.out.println("orig to scan " + orig_to_scan);
			
			// Draw found location of QR code on image
			Graphics2D g = bufImg.createGraphics();
			
			
			g.setColor(Color.RED);
			g.setStroke(new BasicStroke(10));
			g.drawLine((int) pts[0].getX() + crop_x, (int) pts[0].getY() + crop_y,
					(int) pts[1].getX() + crop_x, (int) pts[1].getY() + crop_y);
			g.drawLine((int) pts[2].getX() + crop_x, (int) pts[2].getY() + crop_y,
					(int) pts[1].getX() + crop_x, (int) pts[1].getY() + crop_y);
			
			
			g.setColor(Color.BLUE);
			g.transform(orig_to_scan);
			g.drawLine((int) default_box1[0][0], (int) default_box1[0][1],
					(int) default_box1[1][0], (int) default_box1[1][1]);
			g.drawLine((int) default_box1[2][0], (int) default_box1[2][1],
					(int) default_box1[1][0], (int) default_box1[1][1]);
			
			g.drawLine((int) default_box2[0][0], (int) default_box2[0][1],
					(int) default_box2[1][0], (int) default_box2[1][1]);
			g.drawLine((int) default_box2[2][0], (int) default_box2[2][1],
					(int) default_box2[1][0], (int) default_box2[1][1]);
			
			
			g.dispose();
			
			File outputfile = new File("saved.png");
			ImageIO.write(bufImg, "png", outputfile);

			int IMG_WIDTH = (int) (default_box2[1][0] - default_box2[0][0]);
			int IMG_HEIGHT = (int) (default_box2[2][1] - default_box2[0][1]);
			
			BufferedImage eqTile = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
			Graphics2D gTile = eqTile.createGraphics();
			
			gTile.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			gTile.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
			gTile.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			
			orig_to_scan.invert();
			//gTile.transform(orig_to_scan);
			gTile.translate(-default_box2[0][0], -default_box2[0][1]);
			gTile.drawImage(bufImg, orig_to_scan, null);
			gTile.dispose();
			
			File tileFile = new File("tilefile.png");
			ImageIO.write(eqTile, "png", tileFile);
			
			System.out.println("Done");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ChecksumException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FormatException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoninvertibleTransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
