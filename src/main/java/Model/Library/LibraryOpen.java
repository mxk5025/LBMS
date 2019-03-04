package Model.Library;

import Controller.Request.RequestUtil;
import Model.Book.BookDB;
import Model.Book.BookInfo;
import Model.Checkout.CheckoutDB;
import Model.Checkout.Transaction;
import Model.Visitor.VisitorDB;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The state of the Library when it is open. Checkouts and visits are allowed.
 * 
 * @author Hersh Nagpal
 */
class LibraryOpen implements LibraryState,RequestUtil {

    private TimeKeeper timeKeeper;
    private CheckoutDB checkoutDB;
    private VisitorDB visitorDB;
    private BookDB bookDB;

    public LibraryOpen(TimeKeeper timeKeeper, CheckoutDB checkoutDB, VisitorDB visitorDB, BookDB bookDB) {
        this.timeKeeper = timeKeeper;
        this.checkoutDB = checkoutDB;
        this.visitorDB = visitorDB;
        this.bookDB = bookDB;
    }

    /**
     * Returns the given book for the given visitor.
     * @param visitorID the ID of the visitor checking out the books
     * @param bookIds the isbns of the books to check out
     * @param checkoutDate the current date of checkout
     */
    @Override
    public String checkoutBooks(LocalDateTime checkoutDate, String visitorID, List<String> bookIds) {
        //Check if visitor has outstanding fine
        if (checkoutDB.hasOutstandingFine(visitorID)) {
            int fineAmount = checkoutDB.calculateFine(visitorID);
            //return "borrow,outstanding-fine,amount"
            return BORROW_REQUEST + DELIMITER + OUTSTANDING_FINE + fineAmount+TERMINATOR;
        }
        //Check if visitor has book limit or if request will exceed 5 borrowed book limit
        else if(checkoutDB.hasBookLimit(visitorID) || checkoutDB.willReachBookLimit(visitorID,bookIds.size())){
            return BORROW_REQUEST+DELIMITER+BOOK_LIMIT_EXCEDED+TERMINATOR;
        }
        //Check if visitorID is a valid id
        else if(!visitorDB.validCurrentVisitor(visitorID)){
            return BORROW_REQUEST+DELIMITER+INVALID_VISITOR_ID+TERMINATOR;
        }
        else{
            //Update book number in BookDB
            List<BookInfo> bookInfos = bookDB.borrowBooks(bookIds);
            //Successful checkout
            List<Transaction> transactions = checkoutDB.checkout(checkoutDate, visitorID, bookInfos);
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
    public String beginVisit(String visitorID) {
        return visitorDB.beginVisit(visitorID, timeKeeper.getClock(), timeKeeper.readDate(), timeKeeper.readTime());
    }

} 