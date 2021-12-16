package lab3;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lab2.lab2;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class lab3 {
	public static long threadPoolDownloadTimeWithBlur(String mainPageUrl, int kernelSize, double sigma)
			throws IOException, InterruptedException {
		String url = mainPageUrl;
		Document doc = Jsoup.connect(url).get();
		Elements images = doc.select("a[href*=.png]");

		int workers = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(workers);

		long time = System.nanoTime();
		List<Callable<Boolean>> callableList = new ArrayList<>();
		for (int i = 0; i < images.size(); ++i) {
			String imAddress = url + '/' + images.get(i).attr("href");
			callableList.add(new callableDownloadAndBlur(images.get(i).attr("href"), imAddress,
					kernelSize, sigma));
		}
		List<Future<Boolean>> futures = executor.invokeAll(callableList);
		executor.shutdown();
		return System.nanoTime() - time;
	}

	public static long threadPoolDownloadTimeNoBlur(String mainPageUrl) throws IOException, InterruptedException {
		String url = mainPageUrl;
		Document doc = Jsoup.connect(url).get();
		Elements images = doc.select("a[href*=.png]");

		int workers = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(workers);

		long time = System.nanoTime();
		List<Callable<Boolean>> callableList = new ArrayList<>();
		for(int i = 0; i<images.size(); ++i){
			String imAddress = url + '/' + images.get(i).attr("href");
			callableList.add(new callableDownload(images.get(i).attr("href"), imAddress));
		}
		List<Future<Boolean>> futures = executor.invokeAll(callableList);
		executor.shutdown();
		return System.nanoTime() - time;
	}

	public static class callableDownloadAndBlur implements Callable<Boolean>{
		String fname, imAddress;
		int kernelSize;
		double sigma;

		callableDownloadAndBlur(String fname, String imAddress, int kernelSize, double sigma){
			this.fname = fname;
			this.imAddress = imAddress;
			this.kernelSize = kernelSize;
			this.sigma = sigma;
		}

		@Override
		public Boolean call() throws Exception {
			URL imageURL = new URL(imAddress);
			BufferedImage image = ImageIO.read(imageURL);
			image = gaussianBlur(image, kernelSize, sigma);
			ImageIO.write(image, "png", new File(fname));
			return null;
		}
	}

	public static class callableDownload implements Callable<Boolean> {
		String fname, imAddress;

		callableDownload(String fname, String imAddress){
			this.fname = fname;
			this.imAddress = imAddress;
		}

		@Override
		public Boolean call() throws Exception {
			URL imURL = new URL(imAddress);
			BufferedImage im = ImageIO.read(imURL);
			ImageIO.write(im, "png", new File(fname));
			return null;
		}
	}

	public static long sequentialDownloadTimeWithBlur(String mainPageURL, int kernelSize, double sigma)
			throws IOException{
		String url = mainPageURL;
		Document doc = Jsoup.connect(url).get();
		Elements images = doc.select("a[href*=.png]");

		long time = System.nanoTime();
		for(int i=0; i < images.size(); ++i){
			String imAddress = url + '/' + images.get(i).attr("href");
			BufferedImage im = downloadAndBlur(imAddress, kernelSize, sigma);
			ImageIO.write(im, "png", new File(images.get(i).attr("href")));
		}
		return System.nanoTime() - time;
	}

	public static long sequentialDownloadTimeNoBlur(String mainPageUrl) throws IOException {
		String url = mainPageUrl;
		Document doc = Jsoup.connect(url).get();
		Elements images = doc.select("a[href*=.png]");

		long time = System.nanoTime();
		for(int i=0; i<images.size(); ++i){
			String imAddress = url + '/' + images.get(i).attr("href");
			URL imURL = new URL(imAddress);
			BufferedImage im = ImageIO.read(imURL);
			ImageIO.write(im, "png", new File(images.get(i).attr("href")));
		}
		return System.nanoTime() - time;
	}

	 public static BufferedImage downloadAndBlur(String url, int kernelSize, double sigma) throws IOException {
		 URL imageAddress = new URL(url);
		 BufferedImage image = ImageIO.read(imageAddress);
		 return gaussianBlur(image, kernelSize, sigma);
	}

	static BufferedImage gaussianBlur(BufferedImage im, int kernelSize, double sigma){
		BufferedImage blurred = new BufferedImage(
				im.getColorModel(),
				im.copyData(null),
				im.isAlphaPremultiplied(),
				null
				);

		if(kernelSize % 2 == 0) kernelSize += 1;
		int marginSize = (kernelSize-1)/2;
		int width = im.getWidth(); int height = im.getHeight();
		int minX = marginSize; int maxX = width - marginSize;
		int minY = marginSize; int maxY = height - marginSize;

		double[][] kernel = gaussKernel(kernelSize, sigma);

		for(int x = minX; x<maxX; x++){
			for(int y = minY; y < maxY; y++){
				float[] color = {0, 0, 0};
				int subxStart = x-marginSize;
				int subyStart = y-marginSize;
				for(int xiter=0; xiter < kernelSize; xiter++){
					for(int yiter=0; yiter < kernelSize; yiter++){
						float[] tempColor = {0, 0, 0};
						tempColor = new Color(im.getRGB(subxStart+xiter, subyStart+yiter)).getRGBColorComponents(tempColor);
						for(int i=0; i<3; ++i) color[i] += tempColor[i]*kernel[xiter][yiter];
					}
				}
				Color newColor = new Color(1);
				try {
					newColor = new Color(color[0], color[1], color[2]);
				} catch (java.lang.IllegalArgumentException iae) {
					for(int i=0; i<3; ++i) if(color[i] > 1) color[i] = 1;
					newColor = new Color(color[0], color[1], color[2]);
				}
				blurred.setRGB(x, y, newColor.getRGB());
			}
		}
		return blurred;
	}

	static double[][] gaussKernel(int kernelSize, double sigma){
		if(kernelSize % 2 == 0) kernelSize += 1;
		double mu = ((double)kernelSize-1)/2;
		double[][] kernel = new double[kernelSize][kernelSize];
		double normalizer = 0;
		for(int i=0; i<kernelSize; i++){
			for(int j=0; j<kernelSize; j++){
				double val = gauss((double) i, mu, sigma) * gauss((double) j, mu, sigma);
				kernel[i][j] = val;
				normalizer += val;
			}
		}
		for(int i=0; i<kernelSize; i++) for(int j=0; j<kernelSize; j++) kernel[i][j] /= normalizer;
		return kernel;
	}

	static double gauss(double x, double mu, double sigma){
		return 1./(sigma * Math.sqrt(2*Math.PI)) * Math.exp(-1*Math.pow(x - mu, 2)/(2*Math.pow(sigma, 2)));
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		String mainPage = "http://www.if.pw.edu.pl/~mrow/dyd/wdprir";
		int kernelSize = 3;
		double sigma = 2.5;
		long time = 0;
		System.out.println("Starting...");
		System.out.println("Sequential download, no blur...");
		time = sequentialDownloadTimeNoBlur(mainPage);
		System.out.println("Time: " + (float)time/1000000000 + "s");

		System.out.println("Sequential download, with blur...");
		time = sequentialDownloadTimeWithBlur(mainPage, kernelSize, sigma);
		System.out.println("Time: " + (float)time/1000000000 + "s");

		System.out.println("Thread pool download no blur...");
		time = threadPoolDownloadTimeNoBlur(mainPage);
		System.out.println("Time: " + (float)time/1000000000 + "s");

		System.out.println("Thread pool download with blur...");
		time = threadPoolDownloadTimeWithBlur(mainPage, kernelSize, sigma);
		System.out.println("Time: " + (float)time/1000000000 + "s");
	}
}
