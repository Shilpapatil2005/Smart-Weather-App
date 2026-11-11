import java.io.File;
import MyPackage.WeatherDatabase;

public class DbTest {
  public static void main(String[] args) {
    File base = new File(args[0]);
    base.mkdirs();
    WeatherDatabase db = new WeatherDatabase(base.getAbsolutePath());
    db.clearSearchHistory();
    db.addSearchHistory("London", "United Kingdom");
    db.addSearchHistory(" london ", "United Kingdom");
    db.addSearchHistory("Paris", "France");
    db.addSearchHistory("Paris", "France");
    db.addFavourite("London", "United Kingdom", 51.5, -0.1);
    db.addFavourite(" london ", "United Kingdom", 51.5, -0.1);
    db.addFavourite("Paris", "France", 48.8566, 2.3522);
    System.out.println("SEARCHES=" + db.getRecentSearches());
    System.out.println("FAVS=" + db.getFavouriteCities());
  }
}
