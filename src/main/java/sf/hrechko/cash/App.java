package sf.hrechko.cash;

public class App 
{
    public static void main( String[] args )
    {
        ConsoleUI cli = new ConsoleUI();
        
        do {
        	cli.draw();
        }while(cli.input());
        System.out.println("Приложение закрыто!");
    }
}
