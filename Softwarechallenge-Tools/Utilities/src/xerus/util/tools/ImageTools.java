package xerus.util.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageTools {

	public static BufferedImage readimg(String file){
		try{
			return ImageIO.read(new File(file));
		} catch(IOException e) {
			System.out.println("Image not found: " + e.getMessage());
			return null;
		}
	}
	
	public static FileInputStream in = null;
	public static FileOutputStream out = null;
	public static boolean open(String name){
		try {
			in  = new FileInputStream(name);
			out = new FileOutputStream(name);
			return true;
		} catch(IOException e) {
			System.out.println("Failed to create Stream!");
			System.out.println(e.getMessage());
			return false;
		}
	}
	public static void close(){
		try{
			if (in  != null) in.close();
			if (out != null) out.close();
		} catch(IOException e) {
			System.out.println("Failed to close Stream!");
		}
	}
	
	public class Pixel {
		
		public byte[] value;

		public Pixel() {
			value = new byte[3];
		}

		public Pixel(byte[] v) {
			value = v;
		}
		
		public Pixel(int r, int g, int b){
			value = new byte[]{(byte)r, (byte)g, (byte)b};
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj.getClass() == Pixel.class){
				Pixel p = (Pixel)obj;
				if(this.r()==p.r() && this.g()==p.g() && this.b()==p.b())
					return true;
			}
			return false;
		}
		
		public byte r(){
			return value[0];
		}
		public byte g(){
			return value[1];
		}
		public byte b(){
			return value[2];
		}

	}
	
}
