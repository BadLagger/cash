package sf.hrechko.cash;

public class App {
    public static void main(String[] args) {
	ConsoleUI cli = ConsoleUI.getCLI();

	if (!cli.connectToDb("users.db")) {
	    System.out.println("Ошибка подключения к БД пользователей!");
	}

	do {
	    cli.draw();
	} while (cli.input());
	System.out.println("Приложение закрыто!");
    }
}
