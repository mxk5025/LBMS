package main.java.Model.Library;

import main.java.Controller.Request.RequestUtil;
import main.java.Model.Checkout.CheckoutDB;
import main.java.Model.Checkout.Transaction;
import main.java.Model.Visitor.VisitorDB;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The state of the Library when it is open. Checkouts and visits are allowed.
 * 
 * @author Hersh Nagpal
 */
class LibraryOpen implements LibraryState,RequestUtil {

    /**
     * Returns the given book for the given visitor.
     * @param visitorID the ID of the visitor checking out the books
     * @param bookIds the isbns of the books to check out
     * @param checkoutDate the current date of checkout
     */
    @Override
    public String checkoutBooks(LocalDateTime checkoutDate, String visitorID, List<String> bookIds, CheckoutDB checkoutDB, VisitorDB visitorDB) {
        //Check if visitor has outstanding fine
        if (checkoutDB.hasOutstandingFine(visitorID)) {
            int fineAmount = checkoutDB.calculateFine(visitorID);
            //return "borrow,outstanding-fine,amount"
            return BORROW_REQUEST + DELIMITER + OUTSTANDING_FINE + fineAmount+TERMINATOR;
        }
        //Check if visitor has book limit or if request will exceed 5 borrowed book limit
        else if(checkoutDB.hasBookLimit(visitorID) || checkoutDB.willReachBookLimit(visitorID,bookIds)){
            return BORROW_REQUEST+DELIMITER+BOOK_LIMIT_EXCEDED+TERMINATOR;
        }
        //Check if visitorID is a valid id
        else if(!visitorDB.validCurrentVisitor(visitorID)){
            return BORROW_REQUEST+DELIMITER+INVALID_VISITOR_ID+TERMINATOR;
        }
        else{
            //Successful checkout
            List<Transaction> transactions = checkoutDB.checkout(checkoutDate, visitorID, bookIds);
            //All due dates for transactions made are the same
            String dueDate = transactions.get(0).getDueDate();
            return BORROW_REQUEST+DELIMITER+dueDate+TERMINATOR;
        }
    }

    /**
     * Starts a new visit for the given visitor, which allows them to access the library's services.
     * @param visitorID the visitor whose visit to start
     * @return a formatted string regarding the success of the operation.
     */
    @Override
    public String beginVisit(String visitorID, VisitorDB visitorDB) {
        return visitorDB.beginVisit(visitorID);
    }

} 