
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;

public class exo1 extends Application {

    @Override
    public void start(Stage stage) {
        Label label =new Label("Enter your password:");
        Button button1 = new Button("Login");
        TextField tf= new TextField();
        Button button2 = new Button("Cancel");
        HBox root = new HBox(20,label,tf);
        root.setAlignment(Pos.CENTER);
        HBox root2 = new HBox(20,button1,button2);
        root2.setAlignment(Pos.CENTER);
        VBox oroot = new VBox(root,root2);
        oroot.setAlignment(Pos.CENTER);
        oroot.setSpacing(10);
    
        BorderPane borderPane = new BorderPane(oroot);
        
        Scene scene = new Scene(borderPane,500, 200);

        
        stage.setTitle("amir");
        stage.setScene(scene);
        stage.show();


        Button button3 = new Button("accelerate");
        Button button4 = new Button("slow down");
        HBox root3 = new HBox(20,button3,button4);
        root3.setAlignment(Pos.CENTER);


    }

    public static void main(String[] args) {
        launch();
    }
}
