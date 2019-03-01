package Visitor;

import Checkout.CheckoutDB;
import Checkout.Transaction;
import Library.TimeKeeper;
import Request.RequestUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class VisitorDB implements RequestUtil, Serializable{

    /**
     * All visitors that are registered in the library. (VisitorID, VisitorInfo)
     */
    private Map<String, VisitorInfo> registeredVisitors;

    /**
     * All current visitors in the library. (VisitorID, VisitorInfo)
     */
    private Map<String, VisitorInfo> currentVisitors;

    /**
     * Used for providing visitors with unique IDs
     */
    private int nextVisitorID;

    /**
     * First visitorID provided when library is created
     */
    private final int INITIAL_VISITOR_ID = 1000000000;

    /**
     * Updated when Visitor begins a visit. Used to calculate visit duration
     */
    private LocalDateTime startDayTime;

    /**
     * Create a new visitor database that is empty.
     */
    public VisitorDB() {
        registeredVisitors = new HashMap<>();
        currentVisitors = new HashMap<>();

        //initialize nextVisitorID (All IDs need to be a 10 digit number)
        nextVisitorID = INITIAL_VISITOR_ID;
    }

    /**
     * Register the visitor given properly formatted info.
     * The new visitor is added into the map of visitors.
     * @param info The info needed to create a visitor
     * @return Response to user indicating registration went through or Error.
     */
    public String registerVisitor(String info) {
        //Create new visitorInfo object using info string
        VisitorInfo newVisitorInfo = new VisitorInfo(info);

        //check for duplicate visitorInfo
        for (String currentKey : registeredVisitors.keySet()) {
            //Duplicate visitorInfo found
            if (registeredVisitors.get(currentKey).equals(newVisitorInfo)) {
                return ARRIVE_REQUEST + DELIMITER + DUPLICATE + TERMINATOR;
            }
        }
        //No duplicate was found. Register new visitor.
        int newVisitorID = nextVisitorID;
        nextVisitorID++;
        String newVisitorIDString = Integer.toString(newVisitorID);
        registeredVisitors.put(newVisitorIDString, newVisitorInfo);
        TimeKeeper timeKeeper = TimeKeeper.getInstance();
        String registeredDate = timeKeeper.readDate();
        return REGISTER_REQUEST+DELIMITER+newVisitorIDString
                +DELIMITER+registeredDate+TERMINATOR;
    }

    /**
     * Start a visit given the visitor id.
     * @param visitorID The visitor id to log a visit with
     */
    public String startVisit(String visitorID) {
        //Check if visitor with id already exists in currentVisitors
        if (currentVisitors.containsKey(visitorID)) {
            //Response = "arrive,duplicate";
            return ARRIVE_REQUEST+DELIMITER+DUPLICATE+TERMINATOR;
        }
        //Check if visitor has not registered yet
        else if (!registeredVisitors.containsKey(visitorID)){
            //Response = "arrive,invalid-id";
            return ARRIVE_REQUEST+DELIMITER+INVALID_ID+TERMINATOR;
        }
        //Add visitor to currentVisitors and update its state
        else{
            VisitorInfo visitor = registeredVisitors.get(visitorID);
            currentVisitors.put(visitorID, visitor);
            TimeKeeper timeKeeper = TimeKeeper.getInstance();
            String visitDate = timeKeeper.readDate();
            String visitTime = timeKeeper.readTime();

            //Get LocalDateTime for the startVisit time
            LocalDateTime startVisitDateTime = timeKeeper.getClock();

            //Response = "arrive,visitorID,visitDate,visitStartTime"
            return ARRIVE_REQUEST+DELIMITER+visitorID+DELIMITER
                    +visitDate+DELIMITER+visitTime+TERMINATOR;
            //TODO change visitorInfo state to inLabrary if using state pattern
        }
    }

    /**
     * End a visit that is in progress given the visitor id.
     * @param visitorID The visitor id to end a visit for
     */
    public String endVisit(String visitorID) {
        //Check if visitorID is not currently in current visitors
        if(currentVisitors.containsKey(visitorID)){
            //Response = "arrive,invalid-id;"
            return DEPART_REQUEST+DELIMITER+INVALID_ID+TERMINATOR;
        }

        //Remove visitor from currentVisitors
        currentVisitors.remove(visitorID);

        //Response = "depart,visitorID,visitEndTime,visitDuration"
        TimeKeeper timeKeeper = TimeKeeper.getInstance();
        String visitEndTime = timeKeeper.readTime();

        LocalDateTime endVisitDateTime = timeKeeper.getClock();
        String visitDuration = timeKeeper.calculateDuration(startDayTime, endVisitDateTime);

        return DEPART_REQUEST+DELIMITER+visitorID+DELIMITER+visitEndTime+
                DELIMITER+visitDuration;
    }

    /**
     * Clear all the current visitors that are logged.
     */
    public void clearCurrentVisitors() {
        //TODO go through collection and change state of each visitorInfo
        currentVisitors.clear();
    }

    /**
     * Finds a visitor, determines whether or not they can
     * checkout a book, and then adds a book if they can.
     */
    public boolean checkoutBook(String visitorID, Transaction transaction) {

        //Check if there is a current visitor with visitorID
        if (!currentVisitors.containsKey(visitorID)) {
            //return BORROW_REQUEST+DELIMITER+INVALID_VISITOR_ID;
            //TODO where do we return the Error response for invalid visitor ID for Borrow request
            return false;
        } else {
            VisitorInfo visitor = currentVisitors.get(visitorID);
            //Update fines in visitor's transactions and check for outstanding fines
            boolean hasOutstandingFine = false;
            for (Transaction nextTransaction : visitor.getTransactionList()) {
                nextTransaction.setFine();
                if(nextTransaction.getFineAmount()>0){
                    hasOutstandingFine = true;
                }
            }
            //Check if visitor has already borrowed the max number of books
            if(visitor.getNumberOfTransactions()>Transaction.MAX_NUMBER_OF_TRANSACTIONS){
                //Response = borrow,book-limit-exceeded
                return false;
            }
            //Check if visitor has an outstanding fine in any of their transactions
            else if(hasOutstandingFine){
                return false;
            }
            //Successful transaction, add transaction to Visitor's transactionList
            else{
                visitor.addTransaction(transaction);
                return true;
            }
        }

    }
}
