package sf.hrechko.cash;

import java.util.InputMismatchException;
import java.util.Scanner;

import sf.hrechko.cash.UserDb.CashCategory;
import sf.hrechko.cash.UserDb.User;

public class ConsoleUI {
	/* Перечисления */
	private enum MenuId {

		ENTER_MENU("Меню входа"), 
		AUTORIZATION_MENU("Авторизация пользователя"),
		REGISTRATION_MENU("Регистрация пользователя"), 
		ACCOUNT_MAIN_MENU("Аккаунт пользователя"), 
		ACCOUNT_REVENUE_MENU("Доходы пользователя"), 
		ACCOUNT_SPENDING_MENU("Расходы пользователя"),
		ACCOUNT_REFILL("Пополнение баланса пользователя"),
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
		
		protected void drawHeader(String additionalStr) {
			drawDelimeter();
			System.out.format("%s %s\n", getId().getStr(), additionalStr);
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
		allMenu = new Menu[] { currentMenu, createAutorizationMenu(), createRegistrationMenu(), createAccountMenu(), createRevenueMenu(), createAccountRefillMenu()};
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
	
	public User getCurrentUser() {
		return currentUser;
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
						state = StandartMenuStates.ENTER_LOGIN;
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
		return new Menu(MenuId.ACCOUNT_MAIN_MENU) {
			
			//private User user = ConsoleUI.getCLI().getCurrentUser();
			
			@Override
			public void draw() {
				User user = ConsoleUI.getCLI().getCurrentUser();
				drawHeader(user.getLogin());
				System.out.format("Баланс: %.02f руб.\n\n", user.getBalance());
				System.out.println("1. Доходы.");
				System.out.println("2. Расходы.");
				System.out.println("3. Выйти из аккаунта.\n");
				System.out.format("Введите число соответствующее пункту меню: ");
			}

			@Override
			public boolean input(Scanner inputSrc) {
				String input = inputSrc.next();
				System.out.println();
				if (input.length() == 1) {
					char inputCh = input.charAt(0);
					if (Character.isDigit(inputCh)) {
						switch(inputCh) {
						case '1':
							ConsoleUI.getCLI().setMenuById(MenuId.ACCOUNT_REVENUE_MENU);
							break;
						case '2':
							break;
						case '3':
							ConsoleUI.getCLI().setMenuById(MenuId.ENTER_MENU);
							return true;
						}
					}
				}
				System.out.println("Ошибка ввода!");
				return true;
			}
			
		};
	}
	
	private Menu createRevenueMenu() {
		return new Menu(MenuId.ACCOUNT_REVENUE_MENU) {

			//private User user = ConsoleUI.getCLI().getCurrentUser();
			private boolean drawReport = false;
			
			@Override
			public void draw() {
				User user = ConsoleUI.getCLI().getCurrentUser();
				drawHeader(user.getLogin());
				System.out.format("Баланс: %.02f руб.\n\n", user.getBalance());
				
				if (drawReport) {
					String[] categoryList = user.getIncomeNames();
					double totalCount = 0;
					for (var cat : categoryList) {
						CashCategory income =  user.getIncomeByName(cat);
						double value = income.getValue();
						totalCount += value;
						if ((int)(value * 100) > 0) {
							System.out.format("%s : +%.02f руб.\n", income.getName(), value);
						}
					}
					System.out.format("\nВсего доходов: +%.02f\n\n", totalCount);
					drawReport = false;
				}
				
				System.out.println("1. Отчёт.");
				System.out.println("2. Пополнить.");
				System.out.println("3. Назад.\n");
				System.out.format("Введите число соответствующее пункту меню: ");
			}

			@Override
			public boolean input(Scanner inputSrc) {
				String input = inputSrc.next();
				System.out.println();
				if (input.length() == 1) {
					char inputCh = input.charAt(0);
					if (Character.isDigit(inputCh)) {
						switch(inputCh) {
						case '1':
							drawReport = true;
							return true;
						case '2':
							ConsoleUI.getCLI().setMenuById(MenuId.ACCOUNT_REFILL);
							return true;
						case '3':
							ConsoleUI.getCLI().setMenuById(MenuId.ACCOUNT_MAIN_MENU);
							return true;
						}
					}
				}
				System.out.println("Ошибка ввода!");
				return true;
			}
			
		};
	}
	
	private Menu createAccountRefillMenu() {
		return new Menu(MenuId.ACCOUNT_REFILL) {

			//private User user = ConsoleUI.getCLI().getCurrentUser();
			private String[] catList;
			
			@Override
			public void draw() {
				User user = ConsoleUI.getCLI().getCurrentUser();
				drawHeader(user.getLogin());
				System.out.format("Баланс: %.02f руб.\n\n", user.getBalance());
				catList = user.getIncomeNames();
				for (int i = 0; i < catList.length; ++i) {
					System.out.format("%d. %s.\n", (i+1), catList[i]);
				}
				System.out.format("%d. Добавить категорию.\n", catList.length + 1);
				System.out.format("%d. Удалить  категорию.\n\n", catList.length + 2);
				System.out.format("%d. Вернуться в предыдущее меню.\n\n", catList.length + 3);
				
				System.out.format("Введите число соответствующее пункту меню: ");
			}

			@Override
			public boolean input(Scanner inputSrc) {
				
				try {
					int input = inputSrc.nextInt();
					System.out.println();
					
					if ((input < 1) || (input > (catList.length + 3))) {
						System.out.println("Ошибка ввода!");
						return true;
					}
					
					if (input == catList.length + 1) {
						addCategory(inputSrc);
					} else if (input == catList.length + 2) {
						deleteCategory(inputSrc);
					} else if (input == catList.length + 3) {
						ConsoleUI.getCLI().setMenuById(MenuId.ACCOUNT_REVENUE_MENU);
					} else {
						refillCategory(inputSrc, input);
					}
				} catch (InputMismatchException exp) {
					System.out.println("Ошибка ввода!");
				}
				
				return true;
			}
			
			private void refillCategory(Scanner inputSrc, int catNum) {
				System.out.format("Пополнение категории %s. Введите сумму пополнения: ", catList[catNum-1]);
				try {
					double input = inputSrc.nextDouble();
					System.out.format("Вы собираетесь пополнить категорию %s на %.02f руб. Вы действительно хотите это сделать?\n", catList[catNum-1], input);
					System.out.println("1. Подтвердить.");
					System.out.println("Любой другой ввод приведёт к отмене операции!");
					System.out.format("Введите число соответствующее пункту меню: ");
					String confirm = inputSrc.next();
					System.out.println();
					if (confirm.length() == 1 && confirm.charAt(0) == '1') {
						User user = ConsoleUI.getCLI().getCurrentUser();
						user.setIncome(userDb.new CashCategory(catList[catNum-1], input));
						System.out.format("Операция пополнения категории %s на сумму %.02f успешно выполнена!\n", catList[catNum-1], input);
						return;
					}
					System.out.println("Отмена операции пополнения!");
				} catch (InputMismatchException exp) {
					System.out.println("Ошибка ввода! Отмена операции пополнения!");
				}
			}
			
			private void addCategory(Scanner inputSrc) {
				System.out.println("Введите название категории для добавления: ");
				String catInput = inputSrc.next();
				User user = ConsoleUI.getCLI().getCurrentUser();
				if (user.getIncomeByName(catInput) != null) {
					System.out.println("Такая категория уже существует!");
				} else {
					user.setIncome(userDb.new CashCategory(catInput));
				}
			}
			
			private void deleteCategory(Scanner inputSrc) {
				System.out.println("Введите название категории для удаления: ");
				String catInput = inputSrc.next();
				User user = ConsoleUI.getCLI().getCurrentUser();
				CashCategory cat = user.getIncomeByName(catInput); 
				if (cat == null) {
					System.out.println("Такой категории не существует!");
				} else if (!cat.isErasable()) {
					System.out.println("Эту категорию невозможно удалить!");
				} else {
					double value = cat.getValue();
					System.out.format("\nВНИМАНИЕ! Удаление категории %s со значением %.02f\n", cat.getName(), value);
					System.out.format("1. Удаление с сохранением баланса. При удалении общий баланс не изменится, а значение %.02f будет учтено как доход категории Пополнение.\n", value);
					System.out.format("2. Удаление без сохранения баланса. При удалении общий баланс будет уменьшен на %.02f и учтён, как расход категории Снятие.\n", value);
					System.out.format("Любой другой ввод приведёт к отмене операции удаления!\n");
					System.out.format("Введите число соответствующее пункту меню: ");
					catInput = inputSrc.next();
					System.out.println();
					if (catInput.length() == 1) {
						char inputCh = catInput.charAt(0);
						String catName = cat.getName();
						if (Character.isDigit(inputCh)) {
							switch(inputCh) {
							case '1':
								
								if (user.deleteIncome(cat)) {
									user.setIncome(userDb.new CashCategory("Пополнение", value));
									System.out.format("Удаление %s успешно с сохранением баланса!\n", catName);
								} else {
									System.out.format("Ошибка удаления категории %s\n", catName);
								}
								return;
							case '2':
								if (user.deleteIncome(cat)) {
									user.setIncome(userDb.new CashCategory("Пополнение", value));
									user.setOutcome(userDb.new CashCategory("Снятие", value));
									System.out.format("Удаление %s успешно без сохранения баланса!\n", catName);
								} else {
									System.out.format("Ошибка удаления категории %s\n", catName);
								}
								return;
							default: 
							}
						}
					}
					System.out.println("Отмена операции удаления!");
				}
			}
			
		};
	}

}
