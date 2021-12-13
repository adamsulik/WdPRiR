import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class lab1 {
	public static void main(String[] args){
		generate_mandelbrodt_image(
				512, 512,
				-2.1, 0.6,
				-1.2, 1.2,
				200);
	    System.out.println("Done");
		computationExperiment(5, 200, Arrays.asList(16, 64, 256, 512, 1024, 1536, 2048));
		System.out.println("Done all");
	}
	
	public static void generate_mandelbrodt_image(int xn, int yn,
			double x_min, double x_max,
			double y_min, double y_max,
			int iterations 
			) {
		System.out.println("Generating Mandelbrodt's fractal image");
		// Create list of coordinates x - real numbers, y - imaginary numbers
		List<double[]> coord = get_coords(xn, yn, x_min, x_max, y_min, y_max);
		
		// run calculations
		String wd = "/home/adam/Workspace/WPRiR/src/";
		String fileName = "lab1_mf.txt";
		try{
			FileWriter writer = new FileWriter(wd+fileName);
			String header = "x,y,converged\n";
		    writer.write(header);
			for(double[] c : coord) {
				double[] result = mandelbrodt_pixel(c, iterations);
				String toSave = "" + Double.toString(result[0])+ ',' + result[1] + ',' + (int)result[2] + '\n';
		    	writer.write(toSave);
			}
			writer.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
	
	public static void computationExperiment(int iterations, int convSteps, List<Integer> fractalSizes) {
		String wd = "/home/adam/Workspace/WPRiR/src/";
		String fileName = "lab1_mean_computations.csv";
		try {
			FileWriter writer = new FileWriter(wd+fileName);
			String header = "size,time\n";
			writer.write(header);
			for(int size : fractalSizes) {
				long mean = 0;
				List<double[]> coord = get_coords(size, size, -2.1, 0.6, -1.2, 1.2);
				for(int i=0; i<iterations; i++) {
					long start = System.nanoTime();
					for(double[] c : coord) {
						double[] result = mandelbrodt_pixel(c, convSteps);
					}
					mean += System.nanoTime() - start;
				}
				mean /= iterations;
				String toSave = "" + size + "," + mean + "\n";
				writer.write(toSave);
			}
			writer.close();
			
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
	
	public static List<double[]> get_coords(int xn, int yn,
			double x_min, double x_max,
			double y_min, double y_max){
		List<double[]> coord = new ArrayList<double[]>();
		double x_step = (x_max - x_min)/xn;
		double y_step = (y_max - y_min)/yn;
		if(x_step < 0) x_step *= -1;
		if(y_step < 0) y_step *= -1;
		
		for(double x = x_min; x <= x_max; x += x_step) {
			for(double y = y_min; y <= y_max; y += y_step) {
				double[] c = {x, y};
				coord.add(c);
			}
		}
		return coord;
	}
	
	public static double[] mandelbrodt_pixel(double[] c, int iterations) {
		double re_z = 0;
		double im_z = 0;
		for(int i=0; i<iterations; i++) {
			double re_prev = re_z;
			double im_prev = im_z;
			re_z = re_z_square(re_prev, im_prev) + c[0];
			im_z = im_z_square(re_prev, im_prev) + c[1];
			if(mod_z(re_z, im_z) > 2) break;
		}
		double[] result = new double[3];
		result[0] = c[0]; result[1] = c[1];
		if(mod_z(re_z, im_z) > 2) result[2] = 0;
		else result[2] = 1;
		return result;
	}
	
	
	// Returns real part of z to the second power
	public static double re_z_square(double re_z, double im_z) {
		// z = a + ib => Re z = a, Im z = b 
		//Re (z1**1) = a**2 + b**2
		double a_2 = re_z*re_z;
		double b_2 = im_z*im_z;
		return a_2 - b_2;
	}
	
	// Returns imaginary part of z to the second power
	public static double im_z_square(double re_z, double im_z) {
		// z = a + ib => Re z = a, Im z = b 
		// Im (z1**1) = 2ab
		return 2*re_z*im_z;
	}
	
	public static double mod_z(double re_z, double im_z) {
		// |z|^2 = a^2 + b^2 => |z| = sqrt(a^2 + b^2)
		double mod_z_2 = re_z*re_z + im_z*im_z;
		return Math.sqrt(mod_z_2);
	}
}
