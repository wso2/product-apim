package com.pizzashack;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.pizzashack.beans.MenuItem;

public class PizzaMenu {

	private Set<MenuItem> items = new TreeSet<MenuItem>(new Comparator<MenuItem>() {
		public int compare(MenuItem o1, MenuItem o2) {
			return o1.getName().compareTo(o2.getName());
		}
	});
	
	private static final PizzaMenu instance = new PizzaMenu();
	
	private PizzaMenu() {
		Random rand = new Random();
		DecimalFormat format = new DecimalFormat("#.##");
		
		MenuItem item1 = new MenuItem();
		item1.setName("Chicken Parmesan");
		item1.setDescription("Grilled chicken, fresh tomatoes, feta and mozzarella cheese");
		item1.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item1.setIcon("/images/1.png");
		items.add(item1);
		
		MenuItem item2 = new MenuItem();
		item2.setName("Spicy Italian");
		item2.setDescription("Pepperoni and a double portion of spicy Italian sausage");
		item2.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item2.setIcon("/images/2.png");
		items.add(item2);
		
		MenuItem item3 = new MenuItem();
		item3.setName("Garden Fresh");
		item3.setDescription("Slices onions and green peppers, gourmet " +
				"mushrooms, black olives and ripe Roma tomatoes");
		item3.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item3.setIcon("/images/3.png");
		items.add(item3);
		
		MenuItem item4 = new MenuItem();
		item4.setName("Tuscan Six Cheese");
		item4.setDescription("Six cheese blend of mozzarella, Parmesan, Romano, Asiago and Fontina");
		item4.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item4.setIcon("/images/4.png");
		items.add(item4);
		
		MenuItem item5 = new MenuItem();
		item5.setName("Spinach Alfredo");
		item5.setDescription("Rich and creamy blend of spinach and garlic Parmesan with Alfredo sauce");
		item5.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item5.setIcon("/images/5.png");
		items.add(item5);
		
		MenuItem item6 = new MenuItem();
		item6.setName("BBQ Chicken Bacon");
		item6.setDescription("Grilled white chicken, hickory-smoked bacon and fresh sliced onions in barbeque sauce");
		item6.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item6.setIcon("/images/6.png");
		items.add(item6);
		
		MenuItem item7 = new MenuItem();
		item7.setName("Hawaiian BBQ Chicken");
		item7.setDescription("Grilled white chicken, hickory-smoked bacon, barbeque sauce topped with sweet pine-apple");
		item7.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item7.setIcon("/images/7.png");
		items.add(item7);
		
		MenuItem item8 = new MenuItem();
		item8.setName("Grilled Chicken Club");
		item8.setDescription("Grilled white chicken, hickory-smoked bacon and fresh sliced onions topped with Roma tomatoes");
		item8.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item8.setIcon("/images/8.png");
		items.add(item8);
		
		MenuItem item9 = new MenuItem();
		item9.setName("Double Bacon 6Cheese");
		item9.setDescription("Hickory-smoked bacon, Julienne cut Canadian bacon, Parmesan, " +
				"mozzarella, Romano, Asiago and and Fontina cheese");
		item9.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item9.setIcon("/images/9.png");
		items.add(item9);
		
		MenuItem item10 = new MenuItem();
		item10.setName("Chilly Chicken Cordon Bleu");
		item10.setDescription("Spinash Alfredo sauce topped with grilled chicken, ham, onions and " +
				"mozzarella");
		item10.setPrice(format.format(rand.nextInt(20) + 10 - 0.01));
		item10.setIcon("/images/10.png");
		items.add(item10);
	}
	
	public static PizzaMenu getInstance() {
		return instance;
	}
	
	public MenuItem[] getMenu() {
		return items.toArray(new MenuItem[items.size()]);
	}
}
