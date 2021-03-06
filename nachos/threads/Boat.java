package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{

    static BoatGrader bg;
    static int numChildrenOnOahu;
    static int numAdultsOnOahu;
    static int numChildrenOnMolokai;
    static int numAdultsOnMolokai;
    static String boatLocation;
    static Lock pilotSeat;
    static Lock terminationLock;
    static Condition2 canRowToOahu;
    static Condition2 canRowToMolokai;
    static Condition2 passengerCondition;
    static Condition2 terminationCondition;
    static boolean hasChildPilot;
    static boolean hasPassenger;

    public static void selfTest()
    {
        BoatGrader b = new BoatGrader();
        System.out.println("\n ***Testing Boats with only 2 children***");
        begin(0, 2, b);

    //  System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
    //      begin(1, 2, b);

    //      System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
    //      begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;

        // Instantiate global variables here
        
        numAdultsOnOahu = numChildrenOnOahu = numAdultsOnMolokai = numChildrenOnMolokai = 0;
        boatLocation = "Oahu";
        pilotSeat = new Lock();
        terminationLock = new Lock();
        canRowToMolokai = new Condition2(pilotSeat);
        canRowToOahu = new Condition2(pilotSeat);
        passengerCondition = new Condition2(pilotSeat);
        terminationCondition = new Condition2(terminationLock);
        hasChildPilot = false;
        hasPassenger = false;
        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        for (int i = 0; i< children; i++) {
            KThread child = new KThread(new Runnable() {
                public void run() {
                    ChildItinerary();
                }
            });
            child.fork();
        }

        for (int j = 0; j< adults; j++) {
            KThread adult = new KThread(new Runnable() {
                public void run() {
                    AdultItinerary();
                }
            });         
            adult.setName("Adult");
            adult.fork();
        }

        while (true) {
            if (numChildrenOnMolokai == children && numAdultsOnMolokai == adults) {
                break;
            } else {
                pilotSeat.acquire();
                canRowToOahu.wake();
                pilotSeat.release();
                terminationLock.acquire();
                terminationCondition.sleep();
            }
        }
    }

    static void AdultItinerary()
    {
        /* This is where you should put your solutions. Make calls
           to the BoatGrader to show that it is synchronized. For
           example:
               bg.AdultRowToMolokai();
           indicates that an adult has rowed the boat across to Molokai
        */
        pilotSeat.acquire();
        numAdultsOnOahu++;
        String location = "Oahu";
        while (true) {
            if (location == "Oahu" && Boat.boatLocation == "Oahu") {
                if (numChildrenOnOahu == 1 && hasChildPilot == false) {
                    numAdultsOnOahu--;
                    bg.AdultRowToMolokai();
                    numAdultsOnMolokai++;
                    Boat.boatLocation = "Molokai";
                    location = "Molokai";
                    if (numChildrenOnMolokai != 0) {
                        //there is a child to bring the boat back to Oahu
                        //Adult has been succesfully transported across
                        canRowToOahu.wake();
                        pilotSeat.release();
                        break;
                    } else {
                        numAdultsOnMolokai--;
                        bg.AdultRowToOahu();
                        numAdultsOnOahu++;
                        Boat.boatLocation = "Oahu";
                        location = "Oahu";
                        canRowToMolokai.sleep();
                        continue;
                    }
                } else if (numChildrenOnOahu > 0) {
                    canRowToMolokai.wake();
                } 
            } 
            canRowToMolokai.sleep();
        }
    }

    static void ChildItinerary()
    {
        boolean terminationPossible;
        pilotSeat.acquire();
        numChildrenOnOahu++;
        String location = "Oahu";
        while (true) {
            terminationPossible = false;
            if (location == "Oahu") {
                if (Boat.boatLocation == "Oahu") {
                    if (numChildrenOnOahu == 1 && !hasChildPilot) {
                        canRowToMolokai.wake();
                        canRowToMolokai.sleep();
                    } else if (hasChildPilot && !hasPassenger) {
                        if (numChildrenOnOahu == 2 && numAdultsOnOahu == 0) {
                            terminationPossible = true;
                        }
                        passengerCondition.wake();
                        hasPassenger = true;
                        passengerCondition.sleep();
                        numChildrenOnOahu--;
                        bg.ChildRideToMolokai();
                        numChildrenOnMolokai++;
                        location = "Molokai";
                        hasChildPilot = false;
                        hasPassenger = false;
                        if (terminationPossible) {
                            //possible termination condition
                            //child was the last one to leave Oahu at that time
                            terminationLock.acquire();
                            terminationCondition.wake();
                            terminationLock.release();
                        } else {
                            canRowToOahu.wake();
                        }
                        canRowToOahu.sleep();
                    } else if (!hasChildPilot) {
                        hasChildPilot = true;
                        canRowToMolokai.wake();
                        passengerCondition.sleep();
                        numChildrenOnOahu--;
                        bg.ChildRowToMolokai();
                        numChildrenOnMolokai++;
                        location = "Molokai";
                        passengerCondition.wake();
                        Boat.boatLocation = "Molokai";
                        canRowToOahu.sleep();
                    } else {
                        canRowToMolokai.sleep();
                    }
                } else { 
                    canRowToMolokai.sleep();
                }
            } else {
                if (Boat.boatLocation == "Molokai") {
                    Boat.boatLocation = "Oahu";
                    location = "Oahu";
                    numChildrenOnMolokai--;
                    bg.ChildRowToOahu();
                    numChildrenOnOahu++;
                    canRowToMolokai.wake();
                    canRowToMolokai.sleep();
                } else {
                    canRowToOahu.sleep();
                }
            }
        }
    }

    static void SampleItinerary()
    {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }
    
}
