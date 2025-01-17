package sf.hrechko.cash;

public class ConsoleUI {
    
    public enum Menu {
	
	ENTER_MENU("Меню входа"),
	AUTORIZATION_MENU("Авторизация пользователя"),
	REGISTRATION_MENU("Регистрация пользователя"),
	EMPTY_MENU("");
	
	private String menuStr;
	
	Menu(String name) {
	    menuStr = name;
	}
	
	String getStr() {
	    return menuStr;
	}
    }
    
    public abstract class MenuElement {
	
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
	
	public abstract boolean action();
    }
    
    private Menu currentMenu;
    
    ConsoleUI() {
	currentMenu = Menu.ENTER_MENU;
    }
    
    
}
