import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

// ================== BOOK CLASS ==================
class Book {
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private int totalCopies;
    private int borrowedCopies;

    public Book(String isbn, String title, String author, String genre, int totalCopies) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.borrowedCopies = 0;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public int getTotalCopies() { return totalCopies; }
    public int getBorrowedCopies() { return borrowedCopies; }
    public int getAvailableCopies() { return totalCopies - borrowedCopies; }

    public void borrow() {
        if (borrowedCopies < totalCopies) borrowedCopies++;
    }

    public void returnBook() {
        if (borrowedCopies > 0) borrowedCopies--;
    }
}

// ================== ABSTRACT DATALOADER ==================
abstract class AbstractDataLoader {
    public abstract List<Book> loadBooks() throws Exception;
}

// ================== FILE DATALOADER ==================
class FileDataLoader extends AbstractDataLoader {
    private String filePath;

    public FileDataLoader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public List<Book> loadBooks() throws Exception {
        List<Book> books = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String isbn = parts[0].trim();
                    String title = parts[1].trim();
                    String author = parts[2].trim();
                    String genre = parts[3].trim();
                    int total = Integer.parseInt(parts[4].trim());
                    books.add(new Book(isbn, title, author, genre, total));
                }
            }
        }
        return books;
    }
}

// ================== DATABASE DATALOADER (placeholder) ==================
class DatabaseDataLoader extends AbstractDataLoader {
    private String connectionString;

    public DatabaseDataLoader(String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public List<Book> loadBooks() {
        List<Book> books = new ArrayList<>();
        books.add(new Book("100", "Database Book 1", "DB Author 1", "Technical", 3));
        books.add(new Book("101", "Database Book 2", "DB Author 2", "Fiction", 5));
        return books;
    }
}

// ================== CATALOG CLASS ==================
class Catalog {
    private List<Book> books = new ArrayList<>();

    public void loadFromSource(AbstractDataLoader loader) throws Exception {
        books = loader.loadBooks();
    }

    public List<Book> getAllBooks() {
        return books;
    }

    public Book findByIsbn(String isbn) {
        for (Book b : books) {
            if (b.getIsbn().equalsIgnoreCase(isbn)) return b;
        }
        return null;
    }

    public List<Book> searchBooks(String type, String term) {
        List<Book> results = new ArrayList<>();
        for (Book b : books) {
            switch (type) {
                case "Title":
                    if (b.getTitle().toLowerCase().contains(term.toLowerCase())) results.add(b);
                    break;
                case "Author":
                    if (b.getAuthor().toLowerCase().contains(term.toLowerCase())) results.add(b);
                    break;
                case "Genre":
                    if (b.getGenre().toLowerCase().contains(term.toLowerCase())) results.add(b);
                    break;
                case "ISBN":
                    if (b.getIsbn().equalsIgnoreCase(term)) results.add(b);
                    break;
            }
        }
        return results;
    }

    public boolean borrowBook(String isbn) {
        Book b = findByIsbn(isbn);
        if (b != null && b.getAvailableCopies() > 0) {
            b.borrow();
            return true;
        }
        return false;
    }

    public boolean returnBook(String isbn) {
        Book b = findByIsbn(isbn);
        if (b != null && b.getBorrowedCopies() > 0) {
            b.returnBook();
            return true;
        }
        return false;
    }

    public List<Book> topBorrowed(int n) {
        List<Book> sorted = new ArrayList<>(books);
        sorted.sort((a, b) -> Integer.compare(b.getBorrowedCopies(), a.getBorrowedCopies()));
        return sorted.subList(0, Math.min(n, sorted.size()));
    }
}

// ================== GUI CLASS ==================
public class LMSGUI {
    private Catalog catalog;
    private JFrame frame;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> searchTypeCombo;
    private JTextField filePathField;
    private JComboBox<String> sourceCombo;
    private JTextArea statusArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new LMSGUI().initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void initialize() {
        catalog = new Catalog();

        frame = new JFrame("Library Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());

        frame.setJMenuBar(createMenuBar());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(createDataSourcePanel(), BorderLayout.NORTH);

        createBookTable();
        JScrollPane tableScrollPane = new JScrollPane(bookTable);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        mainPanel.add(createSearchPanel(), BorderLayout.SOUTH);

        statusArea = new JTextArea(3, 80);
        statusArea.setEditable(false);
        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusScrollPane.setBorder(BorderFactory.createTitledBorder("Status"));

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(statusScrollPane, BorderLayout.SOUTH);

        frame.setVisible(true);
        updateStatus("Library Management System initialized. Load data to begin.");
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem loadMenuItem = new JMenuItem("Load Data");
        loadMenuItem.addActionListener(e -> loadData());
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        fileMenu.add(loadMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        JMenu booksMenu = new JMenu("Books");
        JMenuItem borrowMenuItem = new JMenuItem("Borrow Book");
        borrowMenuItem.addActionListener(e -> borrowBook());
        JMenuItem returnMenuItem = new JMenuItem("Return Book");
        returnMenuItem.addActionListener(e -> returnBook());
        JMenuItem displayAllMenuItem = new JMenuItem("Display All Books");
        displayAllMenuItem.addActionListener(e -> displayAllBooks());
        JMenuItem topBorrowedMenuItem = new JMenuItem("Top Borrowed Books");
        topBorrowedMenuItem.addActionListener(e -> showTopBorrowed());
        booksMenu.add(borrowMenuItem);
        booksMenu.add(returnMenuItem);
        booksMenu.add(displayAllMenuItem);
        booksMenu.add(topBorrowedMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(booksMenu);

        return menuBar;
    }

    private JPanel createDataSourcePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Data Source"));

        JLabel sourceLabel = new JLabel("Data Source:");
        sourceCombo = new JComboBox<>(new String[]{"File", "Database"});

        JLabel pathLabel = new JLabel("Path:");
        filePathField = new JTextField(30);
        filePathField.setText("books.txt");

        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> browseForFile());

        JButton loadButton = new JButton("Load Data");
        loadButton.addActionListener(e -> loadData());

        panel.add(sourceLabel);
        panel.add(sourceCombo);
        panel.add(pathLabel);
        panel.add(filePathField);
        panel.add(browseButton);
        panel.add(loadButton);

        return panel;
    }

    private void createBookTable() {
        String[] columnNames = {"ISBN", "Title", "Author", "Genre", "Available", "Total", "Borrowed"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        bookTable = new JTable(tableModel);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookTable.getTableHeader().setReorderingAllowed(false);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Search"));

        JLabel searchLabel = new JLabel("Search:");
        searchField = new JTextField(20);
        searchTypeCombo = new JComboBox<>(new String[]{"Title", "Author", "Genre", "ISBN"});

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());

        JButton clearButton = new JButton("Clear Search");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            displayAllBooks();
        });

        panel.add(searchLabel);
        panel.add(searchField);
        panel.add(searchTypeCombo);
        panel.add(searchButton);
        panel.add(clearButton);

        return panel;
    }

    private void browseForFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadData() {
        try {
            AbstractDataLoader loader;
            String source = (String) sourceCombo.getSelectedItem();
            if ("Database".equals(source)) {
                loader = new DatabaseDataLoader("jdbc:mysql://localhost:3306/library");
            } else {
                String path = filePathField.getText();
                if (path.isEmpty()) {
                    showError("Please specify a file path");
                    return;
                }
                loader = new FileDataLoader(path);
            }
            catalog.loadFromSource(loader);
            displayAllBooks();
            updateStatus("Data loaded successfully from: " + source);
        } catch (Exception e) {
            showError("Failed to load data: " + e.getMessage());
            updateStatus("Error loading data: " + e.getMessage());
        }
    }

    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            showError("Please enter a search term");
            return;
        }
        String searchType = (String) searchTypeCombo.getSelectedItem();
        clearTable();
        List<Book> results = catalog.searchBooks(searchType, searchTerm);
        for (Book book : results) {
            addBookToTable(book);
        }
        updateStatus("Search completed: " + results.size() + " results found for " + searchType + ": " + searchTerm);
    }

    private void displayAllBooks() {
        clearTable();
        for (Book book : catalog.getAllBooks()) {
            addBookToTable(book);
        }
        updateStatus("Displaying all books: " + catalog.getAllBooks().size() + " books");
    }

    private void borrowBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a book to borrow");
            return;
        }
        String isbn = tableModel.getValueAt(selectedRow, 0).toString();
        String title = tableModel.getValueAt(selectedRow, 1).toString();
        if (catalog.borrowBook(isbn)) {
            updateStatus("Book borrowed: " + title + " (ISBN: " + isbn + ")");
            refreshTable();
        } else {
            showError("No copies available for " + title);
        }
    }

    private void returnBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a book to return");
            return;
        }
        String isbn = tableModel.getValueAt(selectedRow, 0).toString();
        String title = tableModel.getValueAt(selectedRow, 1).toString();
        if (catalog.returnBook(isbn)) {
            updateStatus("Book returned: " + title + " (ISBN: " + isbn + ")");
            refreshTable();
        } else {
            showError("All copies are already available for " + title);
        }
    }

    private void showTopBorrowed() {
        String input = JOptionPane.showInputDialog(frame, "Enter number of top borrowed books to show:", "5");
        if (input == null) return;
        try {
            int n = Integer.parseInt(input);
            clearTable();
            List<Book> topBooks = catalog.topBorrowed(n);
            for (Book book : topBooks) {
                addBookToTable(book);
            }
            updateStatus("Displaying top " + n + " borrowed books");
        } catch (NumberFormatException e) {
            showError("Please enter a valid number");
        }
    }

    private void addBookToTable(Book book) {
        tableModel.addRow(new Object[]{
            book.getIsbn(),
            book.getTitle(),
            book.getAuthor(),
            book.getGenre(),
            book.getAvailableCopies(),
            book.getTotalCopies(),
            book.getBorrowedCopies()
        });
    }

    private void clearTable() {
        tableModel.setRowCount(0);
    }

    private void refreshTable() {
        int row = bookTable.getSelectedRow();
        displayAllBooks();
        if (row >= 0 && row < tableModel.getRowCount()) {
            bookTable.setRowSelectionInterval(row, row);
        }
    }

    private void updateStatus(String message) {
        statusArea.append(message + "\n");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
