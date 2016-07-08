package com.kevin.huang.mobilemocap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*import static fj.data.Array.array;
import static fj.pre.Ord.*;
import fj.P2;*/

import android.graphics.drawable.Drawable;

public class Utilities {
	
	static void centerAround(int x, int y, Drawable d) {
		int w = d.getIntrinsicWidth();
		int h = d.getIntrinsicHeight();
		int left = x - w / 2;
		int top = y - h / 2;
		int right = left + w;
		int bottom = top + h;
		d.setBounds(left, top, right, bottom);
	}
	
	static void sortByComparator(double [] array, Integer [] indexes){
		
		//or one can use class Pair implements Comparable<Pair> to store the (value, index) and override its compareTo(Pair other) method
        //return Double.valueOf(this.value).compareTo(other.value); make the pair [] and use Arrays.sort(array)
        /*or Arrays.sort(indexes, new Comparator<Integer>() {
	    @Override 
	    public int compare(final Integer o1, final Integer o2) {
	        return Float.compare(data[o1], data[o2]);
	    }
        });*/
		class ArrayIndexComparator implements Comparator<Integer>
		{
		    private final double[] array;

		    public ArrayIndexComparator(double[] array)
		    {
		        this.array = array;
		    }

		    public Integer[] createIndexArray()
		    {
		        Integer[] indexes = new Integer[array.length];
		        for (int i = 0; i < array.length; i++)
		        {
		            indexes[i] = i; // Autoboxing
		        }
		        return indexes;
		    }

		    @Override
		    public int compare(Integer index1, Integer index2)
		    {
		         // Autounbox from Integer to int to use as array indexes
		        //return array[index1].compareTo(array[index2]);
		    	//if array[index1] < array[index2] then compare return negative value, negative value is okay and sort won't change the place, so the default sequence is ascending
		    	//multiplied to -1 if you need descending sort order
		    	return Double.compare(array[index1], array[index2]);
		    }
		}
		
		ArrayIndexComparator comparator = new ArrayIndexComparator(array);
		indexes = comparator.createIndexArray();
		Arrays.sort(indexes, comparator);
		
	}
	
	//TreeMap solution
	static void sortByTreeMap(double [] array, Integer [] indexes){
		
		Map<Double, List<Integer>> map = new TreeMap<Double, List<Integer>>();
		
	    for (int i = 0; i < array.length; ++i) {
	    	
	    	List<Integer> ind = map.get(array[i]);
	        if(ind == null){
	            ind = new ArrayList<Integer>();
	            map.put(array[i], ind);
	        }
	        ind.add(i);
	    }
	    
	 // Now flatten the list
	    List<Integer> indices = new ArrayList<Integer>();
	    for(List<Integer> arr : map.values()) {
	        indices.addAll(arr);
	    }
	    
	    for(int i = 0; i < indices.size(); i++){
	    	indexes[i] = indices.get(i) ;
	    }
	    //map's value should be now the indexes whose relative value have already been sorted because the key in map are organized
	    //Collection<Integer> indices = map.values();
	    
	}
	
	/*// functional programming
	// Take your array to a stream, pair (zip) each element with its index, 
	// quicksort them by pair order (by the double first, then the int), get the second half of each pair
	// and then take all that to an array
	static void sortByFunction(double [] array, Integer [] indexes){
		indexes = array(array).toStream().zipIndex().sort(p2Ord(doubleOrd, intOrd)).map(P2.<Double, Integer>__2()).toArray();
	}*/
	
	//Quicksort solution
	static void sortByQuick(double [] array, int [] indexes){
		quicksort(array, indexes, 0, indexes.length - 1);
	}

	// quicksort a[left] to a[right]
	public static void quicksort(double[] a, int[] index, int left, int right) {
	    if (right <= left) return;
	    int i = partition(a, index, left, right);
	    quicksort(a, index, left, i-1);
	    quicksort(a, index, i+1, right);
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition(double[] a, int[] index, int left, int right) {
	    int i = left - 1;//seems here we choose the last right element as the partition element
	    int j = right;
	    while (true) {
	        while (less(a[++i], a[right]));      // find item on left to swap, a[right] acts as sentinel
	        while (less(a[right], a[--j])) if (j == left) break; // find item on right to swap don't go out-of-bounds            
	        if (i >= j) break;                  // check if pointers cross
	        exch(a, index, i, j);               // swap two elements into place
	    }
	    //than only reason that previous get loop get terminated is because the partition is finished and [0] to [i] are all small elements and [j=i+1] to [right] are all larger elements then with ++i and --j cause i=j+1 > j so loop finished
	    //when previous loop is finished the left part [0] to [j] should be all less than or equal to a[right] and right part [i=j+1] to [right-1] should be larger than or equal to a[right]
	    exch(a, index, i, right);               // swap with partition element
	    return i;
	}

	// is x < y ?
	private static boolean less(double x, double y) {
	    return (x < y);
	}

	// exchange a[i] and a[j]
	private static void exch(double[] a, int[] index, int i, int j) {
	    double swap = a[i];
	    a[i] = a[j];
	    a[j] = swap;
	    int b = index[i];
	    index[i] = index[j];
	    index[j] = b;
	}

}
