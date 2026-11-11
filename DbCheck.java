import MyPackage.*;
public class DbCheck {
  public static void main(String[] args) {
    WeatherDatabase db = new WeatherDatabase(args[0]);
    db.addSearchHistory("London","GB");
    System.out.println(db.getRecentSearches());
  }
}
