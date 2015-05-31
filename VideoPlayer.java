package videoplayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/*
 @author Peter Yang
 */
public class VideoPlayer extends Application{

    //Index for each video in the list
    //TODO, change data structuring to be a doubly noncircular linked list, probably, maybe???
    private static int currVid = 0;

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        List<File> folder = readFolder("Videos");

        //Creates a separate MediaPlayer for each preloaded video
        ArrayList<MediaPlayer> mediaPlayers = new ArrayList<>();
        for (File videoLink : folder){
            String uri = videoLink.toURI().toString();
            Media media = new Media(uri);
            MediaPlayer player = new MediaPlayer(media);
            mediaPlayers.add(player);
        }

        //Initialization
        MediaPlayer currPlayer = mediaPlayers.get(currVid);
        MediaView view = new MediaView(currPlayer);
        //stage.setTitle(file.getName());
        stage.setTitle("The Show Must Go On");

        final DoubleProperty width = view.fitWidthProperty();
        final DoubleProperty height = view.fitHeightProperty();

        //Sets the video to the size of the window rather than the source
        width.bind(Bindings.selectDouble(view.sceneProperty(), "width"));
        height.bind(Bindings.selectDouble(view.sceneProperty(), "height"));
        view.setPreserveRatio(true);

        //Creating the root and adding elements to it
        Group root = new Group();
        final VBox vbox = new VBox();
        final Slider slider = new Slider();
        vbox.getChildren().add(slider);
        root.getChildren().add(view);
        root.getChildren().add(vbox);

        //Default window size of 800x600
        Scene scene = new Scene(root, 800, 600, Color.BLACK);
        stage.setScene(scene);

        //This line here sure is pretty important
        stage.show();

        currPlayer.setOnReady(new Runnable(){
            @Override
            public void run(){
                /**
                Min width calculation has been removed because it ran into issues when the source video was larger than the display monitor itself
                */
                //int w = player.getMedia().getWidth();
                //int h = player.getMedia().getHeight();
                //stage.setMinWidth(w);
                //stage.setMinHeight(h);
                //vbox.setMinSize(w, h);
                slider.setMin(0);
                slider.setValue(0);
                slider.setMax(currPlayer.getTotalDuration().toSeconds());
                //By default set the slider somewhere far out of view
                vbox.setTranslateY(-1000);
            }
        });
        currPlayer.setOnEndOfMedia(new Runnable(){
            @Override
            public void run(){
                //Goes back to the start; infinite auto-replay
                currPlayer.seek(currPlayer.getStartTime());
            }
        });
        currPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>(){
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue){
                slider.setValue(newValue.toSeconds());
            }
        });
        slider.setOnMouseClicked(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent event){
                currPlayer.seek(Duration.seconds(slider.getValue()));
            }
        });

        //Assigning what keyboard keys do
        root.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>(){
            @Override
            public void handle(KeyEvent event){
                switch (event.getCode()){
                    /**
                     When switching between videos, progress, if any, is not reset This allows the user to make sure the audience does not see any repeated footage
                     */
                    case RIGHT:
                        if (currVid < mediaPlayers.size() - 1){
                            System.out.println("Playing next video");
                            currVid++;
                            view.setMediaPlayer(mediaPlayers.get(currVid));
                            mediaPlayers.get(currVid).play();
                            //Pause past video after successfully changing videos
                            mediaPlayers.get(currVid - 1).pause();
                        }
                        else
                            System.out.println("No more videos in folder");
                        break;
                    case LEFT:
                        if (currVid > 0){
                            System.out.println("Playing previous video");
                            currVid--;
                            view.setMediaPlayer(mediaPlayers.get(currVid));
                            mediaPlayers.get(currVid).play();
                            //Pause past video after successfully changing videos
                            mediaPlayers.get(currVid + 1).pause();
                        }
                        else
                            System.out.println("No more previous videos");
                        break;
                    case SPACE:
                        System.out.println("Toggling play/pause");
                        if (currPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)){
                            currPlayer.pause();
                        }
                        else
                            currPlayer.play();
                        break;
                    case F11:
                        System.out.println("Toggling fullscreen");
                        if (stage.isFullScreen())
                            stage.setFullScreen(false);
                        else
                            stage.setFullScreen(true);
                        break;
                    case ENTER:
                        System.out.println("Showing/hiding slider");
                        if (vbox.getTranslateY() == -1000)
                            vbox.setTranslateY(0);
                        else
                            vbox.setTranslateY(-1000);
                        break;
                    default:
                        System.out.println(event.getCode() + " has not been assigned a function");
                }
            }
        });
    }

    /*
     @param folder path
     @return List<File> of what it finds in there
     */
    public List<File> readFolder(String path) throws IOException{
        List<File> filesInFolder = Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        return filesInFolder;
    }
}