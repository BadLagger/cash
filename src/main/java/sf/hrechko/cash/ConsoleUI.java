package sf.hrechko.cash;

import java.util.Scanner;

import sf.hrechko.cash.UserDb.User;

public class ConsoleUI {
	/* Перечисления */
	private enum MenuId {

		ENTER_MENU("Меню входа"), AUTORIZATION_MENU("Авторизация пользователя"),
		REGISTRATION_MENU("Регистрация пользователя"), ACCOUNT_MAIN_MENU("Аккаунт пользователя"), EMPTY_MENU("");

		private String menuStr;

		MenuId(String name) {
			menuStr = name;
		}

		String getStr() {
			return menuStr;
		}
	}

	private enum Action {
		EXIT_ACTION, CHANGE_MENU_ACTION, IDLE_ACTION
	}

	/* Интерфейсы */
	private interface Actionable {
		public default Action action() {
			return Action.IDLE_ACTION;
		}

		public default MenuId changeMenu() {
			return MenuId.EMPTY_MENU;
		}
	}

	private interface Interactivable {
		public void draw();

		public boolean input(Scanner inputSrc);
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

	private abstract class Menu implements Interactivable {
		private final char DELIMETER = '-';
		private final int DELIMETER_LENGTH = 10;
		private MenuId id;

		public Menu(MenuId id) {
			this.id = id;
		}

		public MenuId getId() {
			return id;
		}

		private void drawDelimeter() {
			for (int i = 0; i < DELIMETER_LENGTH; ++i)
				System.out.format("%c", DELIMETER);
			System.out.println();
		}

		protected void drawHeader() {
			drawDelimeter();
			System.out.println(getId().getStr());
			drawDelimeter();
		}
	}

	private class ChooseMenu extends Menu {

		private MenuElement[] elements;

		public ChooseMenu(MenuId id, MenuElement[] elements) {
			super(id);
			this.elements = elements;
		}

		@Override
		public void draw() {
			drawHeader();
			for (MenuElement element : elements) {
				System.out.println(element.toString());
			}
		}

		@Override
		public boolean input(Scanner inputSrc) {
			System.out.format("Введите число соответствующее пункту меню: ");
			String input = inputSrc.next();
			System.out.println();
			if (input.length() == 1) {
				char inputCh = input.charAt(0);
				if (Character.isDigit(inputCh)) {
					MenuElement menuEl = select(inputCh - '0');
					if (menuEl != null) {
						switch (menuEl.action()) {
						case CHANGE_MENU_ACTION:
							ConsoleUI.getCLI().setMenuById(menuEl.changeMenu());
							break;
						case EXIT_ACTION:
							inputSrc.close();
							return false;
						default:
							;
						}
						return true;
					}
				}
			}
			System.out.println("Ошибка ввода!");
			return true;
		}

		MenuElement select(int order) {
			for (MenuElement element : elements) {
				if (element.getOrder() == order) {
					return element;
				}
			}
			return null;
		}

	}

	private enum StandartMenuStates {
		ENTER_LOGIN, READ_LOGIN, ENTER_PWD, READ_PWD, RETURN_TO_MAIN_MENU, USER_DB_ERROR, USER_NOT_FOUND, PSWD_ERROR,
		USER_EXISTS, USER_OK;
	}

	/* Тело основного класса: Singletone */
	private static Menu currentMenu;
	private static Menu[] allMenu;
	private static ConsoleUI cli = null;
	private Scanner userInput;
	private UserDb userDb = null;
	private User currentUser = null;

	private ConsoleUI() {
		currentMenu = createMainMenu();
		allMenu = new Menu[] { currentMenu, createAutorizationMenu(), createRegistrationMenu(), createAccountMenu()};
		userInput = new Scanner(System.in);
		cli = this;
	}

	public static ConsoleUI getCLI() {
		if (cli == null) {
			cli = new ConsoleUI();
		}
		return cli;
	}

	public boolean connectToDb(String filePath) {
		userDb = new UserDb(filePath);
		return userDb.init();
	}

	public void draw() {
		currentMenu.draw();
	}

	public boolean input() {
		return currentMenu.input(userInput);
	}

	public void setCurrentUser(User user) {
		currentUser = user;
	}

	/* Вспомогательные методы */
	public void setMenuById(MenuId id) {
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
		MenuElement[] elements = { new MenuElement(order++, "Войти") {
			@Override
			public Action action() {
				return Action.CHANGE_MENU_ACTION;
			}

			@Override
			public MenuId changeMenu() {
				return MenuId.AUTORIZATION_MENU;
			}
		}, new MenuElement(order++, "Зарегистрироваться") {
			@Override
			public Action action() {
				return Action.CHANGE_MENU_ACTION;
			}

			@Override
			public MenuId changeMenu() {
				return MenuId.REGISTRATION_MENU;
			}
		}, new MenuElement(order++, "Выход") {
			@Override
			public Action action() {
				userDb.close();
				return Action.EXIT_ACTION;
			}
		}, };

		return new ChooseMenu(MenuId.ENTER_MENU, elements);
	}

	private Menu createAutorizationMenu() {

		return new Menu(MenuId.AUTORIZATION_MENU) {

			private StandartMenuStates state = StandartMenuStates.ENTER_LOGIN;
			private User user = null;

			@Override
			public void draw() {

				if (userDb == null)
					state = StandartMenuStates.USER_DB_ERROR;

				switch (state) {
				case ENTER_LOGIN:
					drawHeader();
					System.out.format("Введите логин: ");
					state = StandartMenuStates.READ_LOGIN;
					break;
				case ENTER_PWD:
					System.out.format("Введите пароль: ");
					state = StandartMenuStates.READ_PWD;
					break;
				case USER_DB_ERROR:
					System.out.println("Ошибка подключения к БД пользователей!");
					state = StandartMenuStates.RETURN_TO_MAIN_MENU;
					break;
				case USER_NOT_FOUND:
					System.out.println("Пользователь с таким именем не найден!");
					state = StandartMenuStates.RETURN_TO_MAIN_MENU;
					break;
				case PSWD_ERROR:
					System.out.println("Неверный пароль!");
					state = StandartMenuStates.RETURN_TO_MAIN_MENU;
					break;
				default:
					;
				}
			}

			@Override
			public boolean input(Scanner inputSrc) {
				switch (state) {
				case READ_LOGIN:
					String login = inputSrc.next();
					state = (userDb.isUserPresent(login)) ? StandartMenuStates.ENTER_PWD
							: StandartMenuStates.USER_NOT_FOUND;
					user = userDb.getUser(login);
					break;
				case READ_PWD:
					String passwd = inputSrc.next();
					if (user.checkPassword(passwd)) {
						ConsoleUI.getCLI().setCurrentUser(user);
						ConsoleUI.getCLI().setMenuById(MenuId.ACCOUNT_MAIN_MENU);
					} else {
						state = StandartMenuStates.PSWD_ERROR;
					}
					break;
				case RETURN_TO_MAIN_MENU:
					state = StandartMenuStates.ENTER_LOGIN;
					ConsoleUI.getCLI().setMenuById(MenuId.ENTER_MENU);
					break;
				default:
					;
				}
				return true;
			}

		};
	}

	private Menu createRegistrationMenu() {
		return new Menu(MenuId.REGISTRATION_MENU) {

			String userName = "";
			String userPassword = "";
			int pwdCount = 0;
			private StandartMenuStates state = StandartMenuStates.ENTER_LOGIN;

			@Override
			public void draw() {
				if (userDb == null)
					state = StandartMenuStates.USER_DB_ERROR;
				switch (state) {
				case ENTER_LOGIN:
					drawHeader();
					System.out.format("Введите логин: ");
					state = StandartMenuStates.READ_LOGIN;
					break;
				case ENTER_PWD:
					if (pwdCount == 0) {
						System.out.format("Введите пароль: ");
					} else {
						System.out.format("Введите пароль ещё раз: ");
					}
					state = StandartMenuStates.READ_PWD;
					break;
				case PSWD_ERROR:
					System.out.println("Пароли не совпадают!");
					state = StandartMenuStates.RETURN_TO_MAIN_MENU;
					break;
				case USER_DB_ERROR:
					System.out.println("Ошибка подключения к БД пользователей!");
					state = StandartMenuStates.RETURN_TO_MAIN_MENU;
					break;
				case USER_EXISTS:
					System.out.println("Пользователь с таким именем существует!");
					state = StandartMenuStates.ENTER_LOGIN;
					break;
				case USER_OK:
					System.out.println("Пользователь успешно добавлен. Возврат в главное меню!");
					state = StandartMenuStates.RETURN_TO_MAIN_MENU;
					break;
				default:;
				}
			}

			@Override
			public boolean input(Scanner inputSrc) {
				switch (state) {
				case READ_LOGIN:
					String login = inputSrc.next();
					if (userDb.isUserPresent(login)) {
						state = StandartMenuStates.USER_EXISTS;
					} else {
						userName = login;
						state = StandartMenuStates.ENTER_PWD;
					}
					break;
				case READ_PWD:
					String pwd = inputSrc.next();
					if (pwdCount == 0) {
						userPassword = pwd;
						pwdCount++;
						state = StandartMenuStates.ENTER_PWD;
					} else {
						if (pwd.equals(userPassword)) {
							userDb.addUser(userDb.new User(userName, userPassword));
							state = StandartMenuStates.USER_OK;
						} else {
							state = StandartMenuStates.PSWD_ERROR;
						}
						pwdCount = 0;
					}
					break;
				case RETURN_TO_MAIN_MENU:
					state = StandartMenuStates.ENTER_LOGIN;
					ConsoleUI.getCLI().setMenuById(MenuId.ENTER_MENU);
					break;
				default:;
				}
				return true;
			}

		};

	}
	
	private Menu createAccountMenu() {
		int order = 1;
		MenuElement[] elements = {  new MenuElement(order++, "Вернуться в главное меню") {
			@Override
			public Action action() {
				return Action.CHANGE_MENU_ACTION;
			}
			
			@Override
			public MenuId changeMenu() {
				return MenuId.ENTER_MENU;
			}
		}, };

		return new ChooseMenu(MenuId.ACCOUNT_MAIN_MENU, elements);
	}

}
