/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package msv_upload_tool;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

import javafx.scene.control.ProgressBar;
import java.io.File;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import java.lang.InterruptedException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;

/**
 *
 * @author isaacsutter
 */
public class FXMLDocumentController implements Initializable {
    
    private Window stage;
    
    private File file;
    
    private Long totalBytes = 0l;
    
    private Task task;
    
    ReadWriteLock lock;
 
    
    @FXML
    private Label label;
    @FXML
    private Button button;
    @FXML
    private Button button2;
    @FXML
    private Label label2;
    @FXML
    private ProgressBar uploadProgress;
    @FXML
    private Label notification;
    @FXML
    private Button cancelUpload;
    
    @FXML
    private void handleButtonAction(ActionEvent event) {
        
        uploadObject();
        
    }
    
    @FXML
    private void cancelUpload(ActionEvent event) {
        
        notification.setText("Cancelling an upload not implemented yet...");
        
    }
    
    @FXML
    private void handleFileOpen(ActionEvent event) {
        
        lock = new ReentrantReadWriteLock();
        
        stage = button2.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        file = fileChooser.showOpenDialog(stage);
        
        if(file.exists()){
            label.setText(file.getAbsolutePath());

            Long size = file.length()/(1024*1024);

            if(size < 1) label2.setText( "SIZE: less than 1 MB" );
            else label2.setText( "SIZE: " + size + " MB" );
            
            button.setDisable(false);
        }
        
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        button.setDisable(true);
        
    }    
    
    
    private void uploadObject() {
        
        
        final Long max = file.length();
        
        task = new Task<Void>() {
            @Override
            protected Void call() {
                
                boolean doLoop = true;
                long total = 0;
                
                while(doLoop){
                    
                    lock.readLock().lock();
                  
                    try{
                        total = totalBytes;
                    }finally{
                        lock.readLock().unlock();
                    }
                    
                    updateProgress(total, max);
                    if(total == max) doLoop = false;

                    try {
                        Thread.sleep(50);                 //1000 milliseconds is one second.
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    
                }
                
         
                updateProgress(-1,max);
             
                this.succeeded();
                return null;
                
                
            }
        };
        
        uploadProgress.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(new EventHandler(){
        
            @Override
            public void handle(Event event){
                
                label.setText("");

                label2.setText("");
                button.setDisable(true);
                button2.setDisable(false);
                
            }
        
        });
        
        
        Thread th = new Thread(task);
        
        th.setDaemon(true);
        
        //disable the buttons
        button.setDisable(true);
        button2.setDisable(true);
        
        th.start();
        
        String existingBucketName = "mstargeneralfiles";
        String keyName            = "duh/"+file.getName();
        String filePath           = file.getAbsolutePath();  
        
        TransferManager tm = new TransferManager(new ProfileCredentialsProvider());        

        // For more advanced uploads, you can create a request object 
        // and supply additional request parameters (ex: progress listeners,
        // canned ACLs, etc.)
        PutObjectRequest request = new PutObjectRequest(
        		existingBucketName, keyName, new File(filePath));
        
        // You can ask the upload for its progress, or you can 
        // add a ProgressListener to your request to receive notifications 
        // when bytes are transferred.
        request.setGeneralProgressListener(new ProgressListener() {
			
            @Override
            public void progressChanged(ProgressEvent progressEvent) {

            System.out.println(progressEvent.toString());
                
            lock.writeLock().lock();

            try{
                totalBytes += progressEvent.getBytesTransferred();
            }finally{
                lock.writeLock().unlock();
            }    


            }
        });
      
        
        // TransferManager processes all transfers asynchronously, 
        // so this call will return immediately.
        Upload upload = tm.upload(request);
        
        
        
    }

    
}
