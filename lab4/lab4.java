package lab4;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;

import static lab4.lab4.swap;

class Sorter implements Callable<Boolean>  {
    int[] arr;
    int beg, n;
    boolean isSortedFlag;

    Sorter(int[] arr, int start, int interval){
        this.isSortedFlag = true;
        this.beg = start;
        this.n = interval;
        this.arr = arr;
    }

    @Override
    public Boolean call() throws Exception {
        // Perform Bubble sort
        isSortedFlag = true;
        // Perform Bubble sort on odd indexed element
        for (int i=1; i<=n-2; i=i+2){
            if (arr[i+beg] > arr[i+beg+1]){
                swap(arr, i+beg, i+beg+1);
                isSortedFlag = false;
            }
        }

        // Perform Bubble sort on even indexed element
        for (int i=0; i<=n-2; i=i+2){
            if (arr[i+beg] > arr[i+beg+1]){
                swap(arr, i+beg, i+beg+1);
                isSortedFlag = false;
            }
        }
        return isSortedFlag;
    }
}

public class lab4 {

    // based on brick-sort algorithm from GeeksForGeeks but extended with sorting on intervals
    public static void oddEvenSort(int arr[], int beg, int interval_length){
        boolean isSorted = false; // Initially array is unsorted
        int n = interval_length;

        while (!isSorted){
            isSorted = true;

            // Perform Bubble sort on odd indexed element
            for (int i=1; i<=n-2; i=i+2){
                if (arr[i+beg] > arr[i+beg+1]){
                    swap(arr, i+beg, i+beg+1);
                    isSorted = false;
                }
            }

            // Perform Bubble sort on even indexed element
            for (int i=0; i<=n-2; i=i+2){
                if (arr[i+beg] > arr[i+beg+1]){
                    swap(arr, i+beg, i+beg+1);
                    isSorted = false;
                }
            }
        }
    }

    public static void parallelOddEvenSort(int arr[]) throws InterruptedException, ExecutionException {
        boolean isSorted = false; // Initially array is unsorted

        int workers = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(workers);

        int interval_length = (int) Math.floor((double) arr.length / workers);
        if(interval_length % 2 != 0) interval_length -= 1;
        int intervals[] = new int[workers];
        for(int i=0; i < workers; ++i) intervals[i] = interval_length;
        if(interval_length*workers != arr.length)
            intervals[intervals.length-1] = Math.abs(interval_length*(workers-1) - arr.length);

        int starts[] = new int[workers];
        int endsum = 0;
        for(int i=0; i < workers; ++i){
            starts[i] = endsum;
            endsum += intervals[i]; // Only the last interval differs
        }
        List<Callable<Boolean>> sorters = new ArrayList<>();
        for(int i=0; i<workers; ++i){
            if(i < workers - 1) sorters.add(new Sorter(arr, starts[i], intervals[i]+1));
            else sorters.add(new Sorter(arr, starts[i], intervals[i]));
        }

        // Begin sorting
        while(!isSorted){
            isSorted = true;
            List<Future<Boolean>> oddFutures = executor.invokeAll(sorters);
            for(int i=0; i<oddFutures.size(); ++i) if (!oddFutures.get(i).get()) isSorted = false;
        }
        executor.shutdown();
    }

    public static void swap(int[] list, int a, int b){
        int buf = list[a];
        list[a] = list[b];
        list[b] = buf;
    }

    public static void printArray(int[] array){
        for(int i=0; i < array.length; i++) System.out.print("" + array[i] + " ");
        System.out.println("");
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        Random rand = new Random();
        int sizeOfArray = 28;
        int minInt = 1;
        int maxInt = 100;

        // Przykłady sortowania
        int[] myArr = new int[sizeOfArray];

        for(int i=0; i < sizeOfArray; i++) myArr[i] = rand.nextInt((maxInt - minInt) + 1) + minInt;
        System.out.println("Sorting by non-parallel sorter");
        printArray(myArr);
        oddEvenSort(myArr, 0, myArr.length);
        printArray(myArr);

        for(int i=0; i < sizeOfArray; i++) myArr[i] = rand.nextInt((maxInt - minInt) + 1) + minInt;
        System.out.println("Sorting by parallel sorter");
        printArray(myArr);
        parallelOddEvenSort(myArr);
        printArray(myArr);

        int toAverage = 10;
        minInt = 1;
        maxInt = Integer.MAX_VALUE;
        List<Long> size_stamps = new ArrayList<>();
        List<Long> time_stamps = new ArrayList<>();
        List<String> tag_stamps = new ArrayList<>();

        FileWriter writer = new FileWriter("/home/adam/Workspace/WPRiR/src/lab4/results.csv");
        String header = "tag,size,time\n";
        writer.write(header);

        for(int i=0; i<5; ++i){
            int size = (int) Math.pow(10, i+1);
            System.out.printf("Computing for size of %d\n", size);
            int[] toSort = new int[size];
            for(int ii=0; ii<toAverage; ++ii){
                // Parallel
                for(int j=0; j < size; j++) toSort[j] = rand.nextInt((maxInt - minInt) + 1) + minInt;
                long startA = System.currentTimeMillis();
                parallelOddEvenSort(toSort);
                long endA = System.currentTimeMillis() - startA;

                // Non-parallel
                for(int j=0; j < size; j++) toSort[j] = rand.nextInt((maxInt - minInt) + 1) + minInt;
                long startB = System.currentTimeMillis();
                oddEvenSort(toSort, 0 , size);
                long endB = System.currentTimeMillis() - startB;

                // Java-built-in
                for(int j=0; j < size; j++) toSort[j] = rand.nextInt((maxInt - minInt) + 1) + minInt;
                long startC = System.currentTimeMillis();
                Arrays.sort(toSort);
                long endC = System.currentTimeMillis() - startC;

                String to_save = "parallel," + size + "," + endA + "\n" +
                        "non-parallel," + size + "," + endB + "\n"+
                        "java-arrays," + size + "," + endC + "\n";
                writer.write(to_save);
            }
        }
        writer.close();
    }
}
