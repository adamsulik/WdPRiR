package lab2;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.Arrays;

public class lab2 {
	
	public static void main(String[] args) throws InterruptedException{
		String wd = "/home/adam/Workspace/WPRiR/src/";
		String fileName = "lab2_mf_threadlist.txt";
		int size = 1024;
		int workers = Runtime.getRuntime().availableProcessors();
		int[][] mandelbrodt = threadMandelbrodt(
				-2.1, 0.6,
				-1.2, 1.2,
				size, size,
				200,
				workers);
		saveMandelbrodt(mandelbrodt, size, size, wd+fileName);
		System.out.println("Done drawing!");
		
		System.out.println("Computation time experiment...");
		fileName = "lab2_mean_computations_threadlist.csv";
		computationTimeExperimentThreadList(
				5, 200,
				Arrays.asList(16, 64, 256, 512, 1024, 1536, 2048),
				wd+fileName);
		
		System.out.println("Computation time experiment on threads pool...");
		fileName = "lab2_mean_computations_threadpool.csv";
		computationTimeExperimentThreadPool(
				5, 200,
				Arrays.asList(16, 64, 256, 512, 1024, 1536, 2048),
				wd+fileName);
		System.out.println("Done!");
	}
	
	public static void saveMandelbrodt(int[][] mandelbrodt, int sizeX, int sizeY, String path) {
		try{
			FileWriter writer = new FileWriter(path);
			String header = "x,y,converged\n";
		    writer.write(header);
			for(int i = 0; i < sizeX; ++i) {
				for(int j = 0; j < sizeY; ++j) {
					String toSave = "" + i + "," + j + "," + mandelbrodt[j][i] +  '\n';
			    	writer.write(toSave);
				}
			}
			writer.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
	
	public static void computationTimeExperimentThreadList(
			int iterations, int convSteps,
			List<Integer>fractalSizes,
			String fname) {
		try {
			int workers = Runtime.getRuntime().availableProcessors();
			FileWriter writer = new FileWriter(fname);
			String header = "size,time\n";
			writer.write(header);
			for(int size : fractalSizes) {
				long mean = 0;
				for(int i=0; i<iterations; i++) {
					long start = System.nanoTime();
					int[][] mandelbrodt = threadMandelbrodt(
							-2.1, 0.6,
							-1.2, 1.2,
							size, size,
							200,
							workers);
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
	
	public static void computationTimeExperimentThreadPool(
			int iterations, int convSteps,
			List<Integer>fractalSizes,
			String fname) throws InterruptedException {
		try {
			int workers = Runtime.getRuntime().availableProcessors();
			ExecutorService executor = Executors.newFixedThreadPool(workers);

			double x0 = -2.1; double xk = 0.6;
			double y0 = -1.2; double yk = -1.2;
			
			FileWriter writer = new FileWriter(fname);
			String header = "size,time\n";
			writer.write(header);
			
			for(int size : fractalSizes) {
				long mean = 0;
				for(int counter=0; counter<iterations; counter++) {
					long start = System.nanoTime();
					
					int startStep = (int) size/workers;
					double xStep = (xk - x0) / size;
					double yStep = (yk - y0) / size;
					
					int[][] mandelbrodt = new int[size][startStep*workers];
					
					List<List<Integer>> supXList = new ArrayList<List<Integer>>();
					for(int i = 0; i < workers; i++) supXList.add(new ArrayList<Integer>());
					for(int i = 0; i < size; i++) {
						supXList.get(i%workers).add(i);
					}
					
					List<Integer> yarr = new ArrayList<Integer>();
					for(int i = 0; i < size; ++i) yarr.add(i);
					List<Callable<Boolean>> callableList = new ArrayList<>();
					
					for(int i = 0; i< workers; ++i) callableList.add(new callableMandelBrodtPixel(
							supXList.get(i), yarr,
							x0, y0,
							xStep, yStep,
							mandelbrodt,
							200));
					List<Future<Boolean>> futures = executor.invokeAll(callableList);					
					mean += System.nanoTime() - start;
				}
				mean /= iterations;
				String toSave = "" + size + "," + mean + "\n";
				writer.write(toSave);
			}
			executor.shutdown();
			writer.close();
			
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
	
	public static int[][] threadMandelbrodt(
			double x0, double xk,
			double y0, double yk,
			int width, int height,
			int iterations,
			int threadsNum){
		int startStep = (int) width/threadsNum;
		double xStep = (xk - x0) / width;
		double yStep = (yk - y0) / height;
		
		int[][] mandelbrodt = new int[height][startStep*threadsNum];
		
		List<List<Integer>> supXList = new ArrayList<List<Integer>>();
		for(int i = 0; i < threadsNum; i++) supXList.add(new ArrayList<Integer>());
		for(int i = 0; i < width; i++) {
			supXList.get(i%threadsNum).add(i);
		}
		
		List<Integer> yarr = new ArrayList<Integer>();
		for(int i = 0; i < height; ++i) yarr.add(i);
		List<Thread> threadsList = new ArrayList<Thread>();
		
		for(int i = 0; i< threadsNum; ++i) threadsList.add(new Thread(new runnableMandelBrodtPixel(
				supXList.get(i), yarr,
				x0, y0,
				xStep, yStep,
				mandelbrodt,
				iterations)));
		
		for(Thread thread : threadsList) thread.start();
		try{for(Thread thread : threadsList) thread.join();} catch (InterruptedException e) {}
		
		return mandelbrodt;
	}
	
	
	public static class runnableMandelBrodtPixel implements Runnable{
		List<Integer> xarr, yarr;
		double x0, y0;
		double xStep, yStep;
		int[][] mandelbrodt;
		int iterations;
		
		public runnableMandelBrodtPixel(
				List<Integer> xarr, List<Integer> yarr,
				double x0, double y0,
				double xStep, double yStep,
				int[][] mandelbrodt,
				int iterations){
			this.xarr = xarr; this.yarr = yarr;
			this.x0 = x0; this.y0 = y0;
			this.xStep = xStep; this.yStep = yStep;
			this.mandelbrodt = mandelbrodt;
			this.iterations = iterations;
		}
		
		public void generate() {
			for(int x : xarr) {
				for(int y : yarr) {
					double[] c = {x0+xStep*x, y0+yStep*y};
					double[] result = mandelbrodt_pixel(c, iterations);
					mandelbrodt[y][x] = (int) result[2];
				}
			}
		}
		public void run() {generate();}
		public Boolean call() {
			generate();
			return null;
		}		
	}
	
	public static class callableMandelBrodtPixel implements Callable<Boolean> {
		List<Integer> xarr, yarr;
		double x0, y0;
		double xStep, yStep;
		int[][] mandelbrodt;
		int iterations;
		
		public callableMandelBrodtPixel(
				List<Integer> xarr, List<Integer> yarr,
				double x0, double y0,
				double xStep, double yStep,
				int[][] mandelbrodt,
				int iterations){
			this.xarr = xarr; this.yarr = yarr;
			this.x0 = x0; this.y0 = y0;
			this.xStep = xStep; this.yStep = yStep;
			this.mandelbrodt = mandelbrodt;
			this.iterations = iterations;
		}
		
		public void generate() {
			for(int x : xarr) {
				for(int y : yarr) {
					double[] c = {x0+xStep*x, y0+yStep*y};
					double[] result = mandelbrodt_pixel(c, iterations);
					mandelbrodt[y][x] = (int) result[2];
				}
			}
		}

		public Boolean call() {
			generate();
			return null;
		}		
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
