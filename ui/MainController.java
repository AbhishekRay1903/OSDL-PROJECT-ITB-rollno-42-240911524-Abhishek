package com.osdl.ui;

import com.osdl.dao.BillDao;
import com.osdl.dao.GuestDao;
import com.osdl.dao.HotelServiceDao;
import com.osdl.dao.RoomDao;
import com.osdl.dao.StayDao;
import com.osdl.model.BillSummary;
import com.osdl.model.DraftLine;
import com.osdl.model.Guest;
import com.osdl.model.HotelService;
import com.osdl.model.PaymentMethod;
import com.osdl.model.Room;
import com.osdl.model.RoomCategory;
import com.osdl.model.Stay;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {

    private static final DateTimeFormatter STAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GuestDao guestDao = new GuestDao();
    private final RoomDao roomDao = new RoomDao();
    private final HotelServiceDao hotelServiceDao = new HotelServiceDao();
    private final StayDao stayDao = new StayDao();
    private final BillDao billDao = new BillDao();
    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "db-worker");
        t.setDaemon(true);
        return t;
    });

    private final ObservableList<DraftLine> draftLines = FXCollections.observableArrayList();

    @FXML
    private TabPane mainTabs;

    @FXML
    private TableView<Guest> guestTable;

    @FXML
    private TableView<Room> roomTable;

    @FXML
    private TableView<HotelService> serviceTable;

    @FXML
    private TableView<Stay> stayTable;

    @FXML
    private TableView<BillSummary> billTable;

    @FXML
    private TableView<DraftLine> draftLineTable;

    @FXML
    private TextField guestNameField;

    @FXML
    private TextField guestPhoneField;

    @FXML
    private TextField roomNumberField;

    @FXML
    private ComboBox<RoomCategory> roomCategoryCombo;

    @FXML
    private TextField roomRateField;

    @FXML
    private TextField serviceCodeField;

    @FXML
    private TextField serviceNameField;

    @FXML
    private TextField servicePriceField;

    @FXML
    private ComboBox<Guest> checkInGuestCombo;

    @FXML
    private ComboBox<Room> checkInRoomCombo;

    @FXML
    private ComboBox<Stay> billingStayCombo;

    @FXML
    private ComboBox<HotelService> extraServiceCombo;

    @FXML
    private ComboBox<PaymentMethod> paymentCombo;

    @FXML
    private Spinner<Integer> qtySpinner;

    @FXML
    private Label draftTotalLabel;

    @FXML
    private void initialize() {
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99_999, 1));
        paymentCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        paymentCombo.getSelectionModel().selectFirst();
        roomCategoryCombo.setItems(FXCollections.observableArrayList(RoomCategory.values()));
        roomCategoryCombo.getSelectionModel().selectFirst();

        setupGuestTable();
        setupRoomTable();
        setupServiceTable();
        setupStayTable();
        setupBillTable();
        setupDraftLineTable();

        draftLines.addListener((ListChangeListener<DraftLine>) c -> updateDraftTotal());
        draftLineTable.setItems(draftLines);

        onRefresh();
    }

    private void setupGuestTable() {
        TableColumn<Guest, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        TableColumn<Guest, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().phone() == null ? "" : cd.getValue().phone()));
        colName.setPrefWidth(260);
        colPhone.setPrefWidth(180);
        guestTable.getColumns().setAll(colName, colPhone);
    }

    private void setupRoomTable() {
        TableColumn<Room, String> colNum = new TableColumn<>("Room #");
        colNum.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().roomNumber()));
        TableColumn<Room, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().category().name()));
        TableColumn<Room, String> colRate = new TableColumn<>("Nightly rate");
        colRate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().nightlyRate().toPlainString()));
        TableColumn<Room, String> colSt = new TableColumn<>("Status");
        colSt.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));
        roomTable.getColumns().setAll(colNum, colCat, colRate, colSt);
    }

    private void setupServiceTable() {
        TableColumn<HotelService, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().code()));
        TableColumn<HotelService, String> colName = new TableColumn<>("Service");
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        TableColumn<HotelService, String> colPrice = new TableColumn<>("Price");
        colPrice.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().unitPrice().toPlainString()));
        serviceTable.getColumns().setAll(colCode, colName, colPrice);
    }

    private void setupStayTable() {
        TableColumn<Stay, String> colG = new TableColumn<>("Guest");
        colG.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().guestName()));
        TableColumn<Stay, String> colR = new TableColumn<>("Room");
        colR.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().roomNumber()));
        TableColumn<Stay, String> colC = new TableColumn<>("Category");
        colC.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().roomCategory().name()));
        TableColumn<Stay, String> colIn = new TableColumn<>("Check-in");
        colIn.setCellValueFactory(cd -> new SimpleStringProperty(STAY_TIME.format(cd.getValue().checkIn())));
        TableColumn<Stay, String> colRate = new TableColumn<>("Rate/night");
        colRate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().nightlyRate().toPlainString()));
        TableColumn<Stay, String> colSt = new TableColumn<>("Status");
        colSt.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));
        stayTable.getColumns().setAll(colG, colR, colC, colIn, colRate, colSt);
    }

    private void setupBillTable() {
        TableColumn<BillSummary, String> colId = new TableColumn<>("Bill #");
        colId.setCellValueFactory(cd -> new SimpleStringProperty(Long.toString(cd.getValue().id())));
        TableColumn<BillSummary, String> colWhen = new TableColumn<>("Date");
        colWhen.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().billDate() == null ? "" : STAY_TIME.format(cd.getValue().billDate())));
        TableColumn<BillSummary, String> colGuest = new TableColumn<>("Guest");
        colGuest.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().guestName()));
        TableColumn<BillSummary, String> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().roomNumber()));
        TableColumn<BillSummary, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().total().toPlainString()));
        TableColumn<BillSummary, String> colPay = new TableColumn<>("Payment");
        colPay.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().paymentMethod().name()));
        billTable.getColumns().setAll(colId, colWhen, colGuest, colRoom, colTotal, colPay);
    }

    private void setupDraftLineTable() {
        TableColumn<DraftLine, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().code()));
        TableColumn<DraftLine, String> colItem = new TableColumn<>("Item");
        colItem.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().itemName()));
        TableColumn<DraftLine, String> colQty = new TableColumn<>("Qty");
        colQty.setCellValueFactory(cd -> new SimpleStringProperty(Integer.toString(cd.getValue().qty())));
        TableColumn<DraftLine, String> colUnit = new TableColumn<>("Unit");
        colUnit.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().unitPrice().toPlainString()));
        TableColumn<DraftLine, String> colLine = new TableColumn<>("Line total");
        colLine.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().lineTotal().toPlainString()));
        draftLineTable.getColumns().setAll(colCode, colItem, colQty, colUnit, colLine);
    }

    @FXML
    private void onRefresh() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Guest> guests = guestDao.findAll();
                List<Room> rooms = roomDao.findAll();
                List<Room> available = roomDao.findAvailable();
                List<HotelService> allServices = hotelServiceDao.findAll();
                List<HotelService> extras = hotelServiceDao.findBillableExtras();
                List<Stay> activeStays = stayDao.findActiveStays();
                List<BillSummary> bills = billDao.findAllSummaries();
                Platform.runLater(() -> {
                    guestTable.setItems(FXCollections.observableArrayList(guests));
                    roomTable.setItems(FXCollections.observableArrayList(rooms));
                    serviceTable.setItems(FXCollections.observableArrayList(allServices));
                    stayTable.setItems(FXCollections.observableArrayList(activeStays));
                    billTable.setItems(FXCollections.observableArrayList(bills));
                    checkInGuestCombo.setItems(FXCollections.observableArrayList(guests));
                    checkInRoomCombo.setItems(FXCollections.observableArrayList(available));
                    billingStayCombo.setItems(FXCollections.observableArrayList(activeStays));
                    extraServiceCombo.setItems(FXCollections.observableArrayList(extras));
                });
                return null;
            }
        };
        task.setOnFailed(e -> Platform.runLater(() -> alertError(task.getException().getMessage())));
        dbExecutor.execute(task);
    }

    @FXML
    private void onAddGuest() {
        String name = guestNameField.getText() == null ? "" : guestNameField.getText().strip();
        if (name.isEmpty()) {
            alertError("Guest name is required.");
            return;
        }
        String phone = guestPhoneField.getText() == null ? "" : guestPhoneField.getText().strip();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                guestDao.insert(name, phone);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            guestNameField.clear();
            guestPhoneField.clear();
            onRefresh();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> alertError(task.getException().getMessage())));
        dbExecutor.execute(task);
    }

    @FXML
    private void onAddRoom() {
        String num = roomNumberField.getText() == null ? "" : roomNumberField.getText().strip();
        if (num.isEmpty()) {
            alertError("Room number is required.");
            return;
        }
        RoomCategory cat = roomCategoryCombo.getSelectionModel().getSelectedItem();
        if (cat == null) {
            alertError("Select a room category.");
            return;
        }
        BigDecimal rate;
        try {
            rate = new BigDecimal(roomRateField.getText().strip()).setScale(2, RoundingMode.HALF_UP);
            if (rate.signum() <= 0) {
                throw new NumberFormatException();
            }
        } catch (Exception ex) {
            alertError("Enter a valid nightly rate.");
            return;
        }
        RoomCategory catFinal = cat;
        BigDecimal rateFinal = rate;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                roomDao.insert(num, catFinal, rateFinal);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            roomNumberField.clear();
            roomRateField.clear();
            onRefresh();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> alertError(task.getException().getMessage())));
        dbExecutor.execute(task);
    }

    @FXML
    private void onAddService() {
        String code = serviceCodeField.getText() == null ? "" : serviceCodeField.getText().strip();
        String svcName = serviceNameField.getText() == null ? "" : serviceNameField.getText().strip();
        if (code.isEmpty() || svcName.isEmpty()) {
            alertError("Service code and name are required.");
            return;
        }
        if ("ROOM".equalsIgnoreCase(code)) {
            alertError("Code ROOM is reserved for automatic room charges.");
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(servicePriceField.getText().strip()).setScale(2, RoundingMode.HALF_UP);
            if (price.signum() < 0) {
                throw new NumberFormatException();
            }
        } catch (Exception ex) {
            alertError("Enter a valid price.");
            return;
        }
        BigDecimal priceFinal = price;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                hotelServiceDao.insert(code.toUpperCase(), svcName, priceFinal);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            serviceCodeField.clear();
            serviceNameField.clear();
            servicePriceField.clear();
            onRefresh();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> alertError(task.getException().getMessage())));
        dbExecutor.execute(task);
    }

    @FXML
    private void onCheckIn() {
        Guest g = checkInGuestCombo.getSelectionModel().getSelectedItem();
        Room r = checkInRoomCombo.getSelectionModel().getSelectedItem();
        if (g == null || r == null) {
            alertError("Select both guest and an available room.");
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                stayDao.checkIn(g.id(), r.id());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(this::onRefresh));
        task.setOnFailed(e -> Platform.runLater(() -> alertError(task.getException().getMessage())));
        dbExecutor.execute(task);
    }

    @FXML
    private void onAddRoomNights() {
        Stay stay = billingStayCombo.getSelectionModel().getSelectedItem();
        if (stay == null) {
            alertError("Select an active stay.");
            return;
        }
        Task<DraftLine> task = new Task<>() {
            @Override
            protected DraftLine call() throws Exception {
                long roomSvcId = hotelServiceDao.findIdByCode("ROOM");
                int nights = computeNights(stay.checkIn());
                BigDecimal unit = stay.nightlyRate();
                BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
                String label = "Room (" + nights + " night" + (nights == 1 ? "" : "s") + ")";
                return new DraftLine(roomSvcId, "ROOM", label, nights, unit, lineTotal);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> draftLines.add(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> alertError(task.getException().getMessage())));
        dbExecutor.execute(task);
    }

    private static int computeNights(LocalDateTime checkIn) {
        long days = ChronoUnit.DAYS.between(checkIn.toLocalDate(), LocalDate.now());
        return (int) Math.max(1, days);
    }

    @FXML
    private void onAddLine() {
        HotelService s = extraServiceCombo.getSelectionModel().getSelectedItem();
        if (s == null) {
            alertError("Select an extra service.");
            return;
        }
        int qty = qtySpinner.getValue();
        BigDecimal lineTotal = s.unitPrice().multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        draftLines.add(new DraftLine(s.id(), s.code(), s.name(), qty, s.unitPrice(), lineTotal));
    }

    @FXML
    private void onRemoveLine() {
        DraftLine selected = draftLineTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            draftLines.remove(selected);
        }
    }

    @FXML
    private void onSaveBill() {
        Stay stay = billingStayCombo.getSelectionModel().getSelectedItem();
        if (stay == null) {
            alertError("Select the stay to checkout.");
            return;
        }
        if (draftLines.isEmpty()) {
            alertError("Add at least one charge (e.g. Add room nights).");
            return;
        }
        PaymentMethod pm = paymentCombo.getSelectionModel().getSelectedItem();
        if (pm == null) {
            alertError("Select a payment method.");
            return;
        }
        List<DraftLine> snapshot = List.copyOf(draftLines);
        long stayId = stay.id();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                billDao.createBillForStay(stayId, snapshot, pm);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            draftLines.clear();
            onRefresh();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> alertError(task.getException().getMessage())));
        dbExecutor.execute(task);
    }

    private void updateDraftTotal() {
        BigDecimal t = draftLines.stream()
                .map(DraftLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        draftTotalLabel.setText("Total: " + t.toPlainString());
    }

    private static void alertError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message == null ? "Unknown error" : message, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
