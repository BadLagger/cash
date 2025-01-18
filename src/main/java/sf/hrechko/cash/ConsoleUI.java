package sf.hrechko.cash;

import java.util.Scanner;

public class ConsoleUI {
/* Перечисления */
	private enum MenuId {

		ENTER_MENU("Меню входа"), 
		AUTORIZATION_MENU("Авторизация пользователя"),
		REGISTRATION_MENU("Регистрация пользователя"),
		EMPTY_MENU("");

		private String menuStr;

		MenuId(String name) {
			menuStr = name;
		}

		String getStr() {
			return menuStr;
		}
	}
	
	private enum Action {
		EXIT_ACTION,
		CHANGE_MENU_ACTION,
		IDLE_ACTION
	}
/* Перечисления */	
	private interface Actionable {
		public default Action action() {
			return Action.IDLE_ACTION;
		}
		public default MenuId changeMenu() {
			return MenuId.EMPTY_MENU;
		}
	}
/* Закрытые классы */
	private class MenuElement implements Actionable {

		private int order;
		private String name;

		public MenuElement(int order, String name) {
			this.order = order;
			this.name = name;
		}

		public int getOrder() {
			return order;
		}

		public String getName() {
			return name;
		}
		
		public String toString() {
			return String.format("%d. %s\n", order, name);
		}
	}
	
	private class Menu {
		private final char DELIMETER = '-';
		private final int DELIMETER_LENGTH = 10; 
		private MenuId id;
		private MenuElement[] elements;
		
		public Menu(MenuId id, MenuElement[] elements) {
			this.id = id;
			this.elements = elements;
		}
		
		public void draw() {
			drawDelimeter();
			System.out.println(id.getStr());
			drawDelimeter();
			for (MenuElement element : elements) {
				System.out.println(element.toString());
			}
		}
		
		public MenuElement select(int order) {
			for (MenuElement element : elements) {
				if (element.getOrder() == order) {
					return element;
				}
			}
			return null;
		}
		
		public MenuId getId() {
			return id;
		}
		
		private void drawDelimeter() {
			for (int i = 0; i < DELIMETER_LENGTH; ++i)
				System.out.format("%c", DELIMETER);
			System.out.println();
		}
	}
	
/* Тело основного класса */
	private Menu currentMenu;
	private Menu[] allMenu;
	private Scanner userInput;

	public ConsoleUI() {
		currentMenu = createMainMenu();
		allMenu = new Menu[] {
				currentMenu,
				createAutorizationMenu(),
				createRegistrationMenu()
		};
		userInput = new Scanner(System.in);
	}
	
	public void draw() {
		currentMenu.draw();
	}
	
	public boolean input() {
		System.out.format("Введите число соответствующее пункту меню: ");
		String input = userInput.next();
		System.out.println();
		if (input.length() == 1) {
		    char inputCh = input.charAt(0);
		    if (Character.isDigit(inputCh)) {
		    	MenuElement menuEl = currentMenu.select(inputCh - '0');
		    	if (menuEl != null) {
		    		switch(menuEl.action()) {
		    		case  CHANGE_MENU_ACTION:
		    			setMenuById(menuEl.changeMenu());
		    			break;
		    		case EXIT_ACTION:
		    			userInput.close();
		    			return false;
		    		default:;
		    		}
			    	return true;
		    	}
		    }
		}
		System.out.println("Ошибка ввода!");
		return true;
	}
/* Вспомогательные закрытые методы */	
	private void setMenuById(MenuId id) {
		for (Menu m : allMenu) {
			if (m.getId() == id) {
				currentMenu = m;
				break;
			}
		}
	}
	
/* Создание Менюшек */	
	private Menu createMainMenu() {
		int order = 1;
		MenuElement[] elements = {
				new MenuElement(order++, "Войти") {
					@Override
					public Action action() {
						return Action.CHANGE_MENU_ACTION;
					}
					@Override
					public MenuId changeMenu() {
						return MenuId.AUTORIZATION_MENU;
					}
				},
				new MenuElement(order++, "Зарегистрироваться") {
					@Override
					public Action action() {
						return Action.CHANGE_MENU_ACTION;
					}
					@Override
					public MenuId changeMenu() {
						return MenuId.REGISTRATION_MENU;
					}
				},
				new MenuElement(order++, "Выход") {
					@Override
					public Action action() {
						return Action.EXIT_ACTION;
					}
				},
		};
		
		return new Menu(MenuId.ENTER_MENU, elements);
	}
	
	private Menu createAutorizationMenu() {
		int order = 1;
		MenuElement[] elements = {
				new MenuElement(order++, "Назад") {
					@Override
					public Action action() {
						return Action.CHANGE_MENU_ACTION;
					}
					@Override
					public MenuId changeMenu() {
						return MenuId.ENTER_MENU;
					}
				},
		};
		
		return new Menu(MenuId.AUTORIZATION_MENU, elements);
	}
	
	private Menu createRegistrationMenu() {
		int order = 1;
		MenuElement[] elements = {
				new MenuElement(order++, "Назад") {
					@Override
					public Action action() {
						return Action.CHANGE_MENU_ACTION;
					}
					@Override
					public MenuId changeMenu() {
						return MenuId.ENTER_MENU;
					}
				},
		};
		
		return new Menu(MenuId.REGISTRATION_MENU, elements);
	}

}
