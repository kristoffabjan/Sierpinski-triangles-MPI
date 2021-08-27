package com.company;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    //----------------Konfiguracija programa--------------------------
    static int n = 5;  //!! awt doesnt allow rendering for n>8, beacuse of int coordinates
    static  int cores = 8;
    static boolean gui = true;

    static int triangles_num = (int) Math.pow( 3, n );
    //running mode: 1. MPI with gui 4.meritve in primerjave
    static int runningMode = 1;
    //----------------------------------------------------------------
    static double windowHeight = 600;  //sirina okna
    static double windowWidth = 800;   //visina okna
    static double startx;  //levo ogljisce x
    static double starty;  //levo ogljisce y
    static double lenght; //dolzina zacetne stranice
    //1 = task
    //3 = complete triangle
    static public void main(String[] args) throws MPIException {
        //testing mode
        if(runningMode == 4){
            for (int i = 5; i < 22; i++) {
                n = i;
                triangles_num = (int) Math.pow( 3, n );
                MPI.Init(args);

                int myrank = MPI.COMM_WORLD.Rank();
                int size = MPI.COMM_WORLD.Size() ;
                cores = size;

                if (myrank == 0){
                    long timing = farmer(size - 1, gui);
                    System.out.println(n + ", " + timing + ", distributed");
                }else{
                    worker(myrank);
                }

                MPI.Finalize();
                System.out.println();

            }
        }else{
            //normal mode
            MPI.Init(args);

            int myrank = MPI.COMM_WORLD.Rank();
            int size = MPI.COMM_WORLD.Size() ;
            cores = size;
            if (myrank == 0){
                farmer(size - 1,gui );
            }else{
                worker(myrank);
            }

            MPI.Finalize();
        }
    }

    static long farmer(int workers, boolean gui){
        //bag of tasks
        long start = System.currentTimeMillis();
        int tag;

        computeLength();
        setStartingPoint();

        BlockingQueue<double[]> tasks = new LinkedBlockingQueue<>();
        BlockingQueue<double[]> triangles = new LinkedBlockingQueue<>();
        //public Task(double x, double y, double length, double level, double max_level, int source, int tag)
        double[] incoming_task = new double[21];
        double[] outgoing_task;

        //send first task to random worker
        int random_worker = (int) ((Math.random() * ((workers + 1) - 1)) + 1);
        double[] first_packet = new double[]{startx, starty, lenght,  (double) 0, (double) n, (double)0, (double)1,
                                        0.0, 0.0,0.0,0.0,0.0,0.0,0.0,
                                        0.0, 0.0,0.0,0.0,0.0,0.0,0.0};
        //tag 1 = task to do
        MPI.COMM_WORLD.Send(first_packet, 0, first_packet.length, MPI.DOUBLE, random_worker, 1);

        int i = 0;
        while ( triangles.size() < triangles_num && i < triangles_num ){
            MPI.COMM_WORLD.Recv(incoming_task, 0,incoming_task.length,MPI.DOUBLE, MPI.ANY_SOURCE,MPI.ANY_TAG);
            tag = (int) incoming_task[6];

            //koncani trikotnik
           if (tag == 3 ){
               //do not fill queue while testing
               if( runningMode == 1){
                   triangles.add(Arrays.copyOfRange(incoming_task, 0, 7));
               }
               i++;
               //System.out.println("Triangle with x coord = " + incoming_task[0] + " , " + incoming_task[1] + " .saved in queue. Triangle = " + triangles.size());
               //send another task if there is any left
              if ( !tasks.isEmpty() ){
                  try {
                      outgoing_task = tasks.take();
                      //poslji naslednji task v vrsti, ce je na voljo
                      random_worker = (int) ((Math.random() * ((workers + 1) - 1)) + 1);
                      MPI.COMM_WORLD.Send(outgoing_task, 0, outgoing_task.length, MPI.DOUBLE, random_worker, 1);
                  } catch (InterruptedException e) {
                      System.out.println("No more tasks in Q. In worker proces elif");
                  }
              }
           //povratni klic rekurzije za nove 3 taske
           }else if ( tag == 1 ){
               //sprejememo povratni klic in ga damo v queue, 3je klici
               for (int j = 0; j < 3 ; j++) {
                   //split all three tasks out of array and add it to tasks queue
                   double[] mini_task = Arrays.copyOfRange(incoming_task, j*7, j*7+7);
                   //create new task, with task on first 7 positions and other are 0
                   double[] new_task = new double[21];
                   for (int k = 0; k < 20; k++) {
                       if (k <= 6){
                           new_task[k] = mini_task[k];
                       }else{
                           new_task[k] = 0.0;
                       }
                   }
                   tasks.add(new_task);
               }

               try {
                   outgoing_task = tasks.take();
                   //poslji naslednji task v vrsti, ce je na voljo
                   random_worker = (int) ((Math.random() * ((workers + 1) - 1)) + 1);
                   MPI.COMM_WORLD.Send(outgoing_task, 0, outgoing_task.length, MPI.DOUBLE, random_worker, 1);
               } catch (InterruptedException e) {
                   //e.printStackTrace();
                   System.out.println("No more tasks in Q. In worker proces elif");
               }
           }else {
               if ( !tasks.isEmpty()){
                   try {
                       outgoing_task = tasks.take();
                       //poslji naslednji task v vrsti, ce je na voljo
                       random_worker = (int) ((Math.random() * ((workers + 1) - 1)) + 1);
                       MPI.COMM_WORLD.Send(outgoing_task, 0, outgoing_task.length, MPI.DOUBLE, random_worker, 1);
                   } catch (InterruptedException e) {
                       //e.printStackTrace();
                       System.out.println("No more tasks in Q. In worker proces elif");
                   }
               }
           }
        }

        //System.out.println("Done with work. there are " + triangles.size() + " triangles done!!!!!!!!!!!!!!!!!!!!");

        double[] choke_array = new double[]{0.0, 0.0,0.0,0.0,0.0,0.0,404.0,
                0.0, 0.0,0.0,0.0,0.0,0.0,0.0,
                0.0, 0.0,0.0,0.0,0.0,0.0,0.0};

        for (int j = 1; j < cores; j++) {
            Status s = MPI.COMM_WORLD.Iprobe(j, MPI.ANY_TAG);
            if(s == null){
                MPI.COMM_WORLD.Send(choke_array, 0, choke_array.length, MPI.DOUBLE, j, 404);
            }else {
                System.out.println("There was another task unfinished in finalize part!!!!!!!!");
                //MPI.COMM_WORLD.Recv(incoming_task, 0, incoming_task.length, MPI.DOUBLE, j, 1);
                //MPI.COMM_WORLD.Send(choke_array, 0, choke_array.length, MPI.DOUBLE, j, 404);
            }
        }
        //System.out.println("Farmer did that many work: " + tasks_done);
        long end = System.currentTimeMillis() - start;

        if (gui){
            //JFrame.setDefaultLookAndFeelDecorated(true);
            JFrame frame = new JFrame("Distributed Sierpinski triangles ");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setBackground(Color.green);
            frame.setResizable(false);
            frame.setSize(800, 600);

            Triangles_panel panel = new Triangles_panel(triangles);
            frame.add(panel);
            frame.pack();
            frame.setVisible(true);
        }
        return end;
    }

    static void worker(int rank){
        double[] packet = new double[21];
        MPI.COMM_WORLD.Recv(packet, 0,packet.length, MPI.DOUBLE, 0, MPI.ANY_TAG);
        int tag = (int) packet[6];
        //System.out.println("Worker "+ rank + " started.");

        while (tag != 404){
            double x = packet[0];
            double y = packet[1];
            double len = packet[2];
            double curr_lvl = (int) packet[3];
            double max_lvl = (int) packet[4];
            int source = (int) packet[5];

            if (curr_lvl == max_lvl){
                //send triangle, tag = 3 = triangle done, send info source and tag=3
                double[] triangle = new double[]{x,y,len, 0.0, 0.0, (double) rank, 3.0,
                                                    0.0, 0.0,0.0,0.0,0.0,0.0,0.0,
                                                    0.0, 0.0,0.0,0.0,0.0,0.0,0.0};
                MPI.COMM_WORLD.Send(triangle, 0, triangle.length, MPI.DOUBLE, 0, 3);
            }else {
                    //kot tag posljemo id workerja, da nam bo poslan nazaj isti task
                    //are those tasks final triangles or another tasks
                    double[] three_tasks = new double[]{x,y,len/2,curr_lvl +1, max_lvl, (double) rank, (double) 1,   //first task
                            x + len/4,y - (int)((Math.sin(Math.PI/3)) * (len/2)), len/2,curr_lvl + 1,max_lvl,(double) rank, (double) 1,           //second
                            x+len/2,y,len/2,curr_lvl+1,max_lvl,(double) rank, (double) 1};                 //third
                    MPI.COMM_WORLD.Send(three_tasks, 0,three_tasks.length, MPI.DOUBLE, 0, source);
            }
            MPI.COMM_WORLD.Recv(packet, 0, packet.length, MPI.DOUBLE, 0, MPI.ANY_TAG);
            tag = (int) packet[6];
        }
        //System.out.println("Worker " + rank + " is done with job: Made " + triangles_done + " triangles." );
    }

    static void setStartingPoint(){
        startx = (windowWidth - lenght)/2;
        double newY = windowHeight - (windowHeight - (int)(lenght*Math.sqrt(3.0)/2.0)) / 2  ;
        starty = newY - (newY/25);
    }

    static void computeLength(){
        double max_tri_height = windowHeight - (windowHeight/10);
        lenght = max_tri_height/(Math.sin(Math.PI/3));

        if (windowWidth <= lenght){
            windowWidth = lenght + 50 ;
        }
    }
}
