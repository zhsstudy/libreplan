/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.web.workreports;

import static org.navalplanner.web.I18nHelper._;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidValue;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.costcategories.entities.TypeOfWorkHours;
import org.navalplanner.business.labels.entities.Label;
import org.navalplanner.business.labels.entities.LabelType;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.workreports.entities.HoursManagementEnum;
import org.navalplanner.business.workreports.entities.WorkReport;
import org.navalplanner.business.workreports.entities.WorkReportLabelTypeAssigment;
import org.navalplanner.business.workreports.entities.WorkReportLine;
import org.navalplanner.business.workreports.entities.WorkReportType;
import org.navalplanner.business.workreports.valueobjects.DescriptionField;
import org.navalplanner.business.workreports.valueobjects.DescriptionValue;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.Level;
import org.navalplanner.web.common.MessagesForUser;
import org.navalplanner.web.common.OnlyOneVisible;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.common.components.Autocomplete;
import org.navalplanner.web.common.components.NewDataSortableColumn;
import org.navalplanner.web.common.components.NewDataSortableGrid;
import org.navalplanner.web.common.entrypoints.IURLHandlerRegistry;
import org.navalplanner.web.common.entrypoints.URLHandler;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.api.Window;

/**
 * Controller for CRUD actions over a {@link WorkReport}
 *
 * @author Diego Pino García <dpino@igalia.com>
 */
public class WorkReportCRUDController extends GenericForwardComposer implements
        IWorkReportCRUDControllerEntryPoints {

    private static final org.apache.commons.logging.Log LOG = LogFactory.getLog(WorkReportCRUDController.class);

    private Window createWindow;

    private Window listWindow;

    private IWorkReportModel workReportModel;

    private IURLHandlerRegistry URLHandlerRegistry;

    private OnlyOneVisible visibility;

    private IMessagesForUser messagesForUser;

    private Component messagesContainer;

    private IWorkReportTypeCRUDControllerEntryPoints workReportTypeCRUD;

    private WorkReportListRenderer workReportListRenderer = new WorkReportListRenderer();

    private OrderedFieldsAndLabelsRowRenderer orderedFieldsAndLabelsRowRenderer = new OrderedFieldsAndLabelsRowRenderer();

    private NewDataSortableGrid listWorkReportLines;

    private Grid headingFieldsAndLabels;

    private Autocomplete autocompleteResource;

    private Textbox txtOrderElement;

    private final static String MOLD = "paging";

    private final static int PAGING = 10;

    private static final String ITEM = "item";

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        listWorkReportLines = (NewDataSortableGrid) createWindow
                .getFellowIfAny("listWorkReportLines");
        messagesForUser = new MessagesForUser(messagesContainer);
        comp.setVariable("controller", this, true);
        final URLHandler<IWorkReportCRUDControllerEntryPoints> handler = URLHandlerRegistry
                .getRedirectorFor(IWorkReportCRUDControllerEntryPoints.class);
        handler.registerListener(this, page);
        getVisibility().showOnly(listWindow);
    }

    /**
     * Show confirm window for deleting {@link WorkReport}
     *
     * @param workReport
     */
    public void showConfirmDelete(WorkReportDTO workReportDTO) {
        WorkReport workReport = workReportDTO.getWorkReport();
        try {

            final String workReportName = formatWorkReportName(workReport);
            int status = Messagebox.show(_("Confirm deleting {0}. Are you sure?", workReportName), "Delete",
                    Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION);
            if (Messagebox.OK == status) {
                workReportModel.remove(workReport);
                Util.reloadBindings(listWindow);
            }
        } catch (InterruptedException e) {
            messagesForUser.showMessage(
                    Level.ERROR, e.getMessage());
            LOG.error(_("Error on removing element: ", workReport.getId()), e);
        }
    }

    private String formatWorkReportName(WorkReport workReport) {
        return workReport.getWorkReportType().getName();
    }

    public List<WorkReportDTO> getWorkReportDTOs() {
        return workReportModel.getWorkReportDTOs();
    }

    private OnlyOneVisible getVisibility() {
        return (visibility == null) ? new OnlyOneVisible(createWindow,
                listWindow)
                : visibility;
    }

    public void saveAndExit() {
        if (save()) {
            goToList();
        }
    }

    public void saveAndContinue() {
        if (save()) {
            goToEditForm(getWorkReport());
        }
    }

    public boolean save() {
        try {
            workReportModel.confirmSave();
            messagesForUser.showMessage(Level.INFO,
                    _("Work report saved"));
            return true;
        } catch (ValidationException e) {
            showInvalidValues(e);
        } catch (Exception e) {
            showInvalidProperty();
        }
        return false;
    }

    /**
     * Shows invalid values for {@link WorkReport} and {@link WorkReportLine}
     * entities
     * @param e
     */
    private void showInvalidValues(ValidationException e) {
        for (InvalidValue invalidValue : e.getInvalidValues()) {
            Object value = invalidValue.getBean();
            if (value instanceof WorkReport) {
                validateWorkReport();
            }
            if (value instanceof WorkReportLine) {
                validateWorkReportLine((WorkReportLine) invalidValue.getBean());
            }
        }
    }

    private void showInvalidProperty() {
        if (getWorkReport() != null) {
            for (WorkReportLine workReportLine : getWorkReport()
                    .getWorkReportLines()) {
                if (!validateWorkReportLine(workReportLine))
                    return;
            }
        }
    }
    /**
     * Validates {@link WorkReport} data constraints
     * @param invalidValue
     */
    private boolean validateWorkReport() {

        if (!getWorkReport().theDateMustBeNotNullIfIsSharedByLines()) {
            Datebox datebox = (Datebox) createWindow.getFellowIfAny("date");
            showInvalidMessage(datebox, _("Date cannot be null"));
            return false;
        }

        if (!getWorkReport().theResourceMustBeNotNullIfIsSharedByLines()) {
            showInvalidMessage(autocompleteResource,
                    _("Resource cannot be null"));
            return false;
        }

        if (!getWorkReport().theOrderElementMustBeNotNullIfIsSharedByLines()) {
            showInvalidMessage(txtOrderElement,
                    _("Order Element code cannot be null"));
            return false;
        }
        return true;
    }

    /**
     * Validates {@link WorkReportLine} data constraints
     *
     * @param invalidValue
     */
    @SuppressWarnings("unchecked")
    private boolean validateWorkReportLine(WorkReportLine workReportLine) {
        if (listWorkReportLines != null) {
            // Find which row contains workReportLine inside listBox
            Row row = findWorkReportLine(listWorkReportLines.getRows().getChildren(),
 workReportLine);
            workReportLine = (WorkReportLine) row.getValue();

            if (row != null) {
                if (getWorkReportType().getDateIsSharedByLines()) {
                    if (!validateWorkReport()) {
                        return false;
                    }
                } else if (workReportLine.getDate() == null) {
                    Datebox date = getDateboxDate(row);
                    String message = _("The date cannot be null");
                    showInvalidMessage(date, message);
                    return false;
                }

                if (getWorkReportType().getResourceIsSharedInLines()) {
                    if (!validateWorkReport()) {
                        return false;
                    }
                } else if (!workReportLine.theResourceMustBeNotNull()) {
                    Autocomplete autoResource = getTextboxResource(row);
                    String message = _("The resource cannot be null");
                    showInvalidMessage(autoResource, message);
                    return false;
                }

                if (getWorkReportType().getOrderElementIsSharedInLines()) {
                    if (!validateWorkReport()) {
                        return false;
                    }
                } else if (!workReportLine.theOrderElementMustBeNotNull()) {
                    Textbox txtOrder = getTextboxOrder(row);
                    String message = _("The order element code cannot be null");
                    txtOrder.setValue("");
                    showInvalidMessage(txtOrder, message);
                    return false;
                }

                if (!workReportLine
                        .theClockStartMustBeNotNullIfIsCalculatedByClock()) {
                    Timebox timeStart = getTimeboxStart(row);
                    String message = _("Time Start cannot be null");
                    showInvalidMessage(timeStart, message);
                    return false;
                }

                if (!workReportLine
                        .theClockFinishMustBeNotNullIfIsCalculatedByClock()) {
                    Timebox timeFinish = getTimeboxFinish(row);
                    String message = _("Time finish cannot be null");
                    showInvalidMessage(timeFinish, message);
                    return false;
                }

                if (workReportLine.getNumHours() == null) {
                    // Locate TextboxOrder
                    Intbox txtHours = getIntboxHours(row);
                    String message = _("Hours cannot be null");
                    showInvalidMessage(txtHours, message);
                    return false;
                }

                if (workReportLine.getTypeOfWorkHours() == null) {
                    // Locate TextboxOrder
                    Autocomplete autoTypeOfHours = getTypeOfHours(row);
                    String message = _("The type of hours cannot be null.");
                    showInvalidMessage(autoTypeOfHours,message);
                    return false;
                }
            }
        }
        return true;
    }

    private void showInvalidMessage(Component comp, String message) {
        throw new WrongValueException(comp, message);
    }

    /**
     * Locates which {@link Row} is bound to {@link WorkReportLine} in
     * rows
     *
     * @param rows
     * @param workReportLine
     * @return
     */
    private Row findWorkReportLine(List<Row> rows,
            WorkReportLine workReportLine) {
        for (Row row : rows) {
            if (workReportLine.equals(row.getValue())) {
                return row;
            }
        }
        return null;
    }

    /**
     * Locates {@link Timebox} time finish in {@link Row}
     * @param row
     * @return
     */
    private Timebox getTimeboxFinish(Row row) {
        int position = row.getChildren().size() - 4;
        return (Timebox) row.getChildren().get(position);
    }

    /**
     * Locates {@link Timebox} time start in {@link Row}
     * @param row
     * @return
     */
    private Timebox getTimeboxStart(Row row) {
        int position = row.getChildren().size() - 5;
        return (Timebox) row.getChildren().get(position);
    }

    /**
     * Locates {@link Autocomplete} type of work hours in {@link Row}
     * @param row
     * @return
     */
    private Autocomplete getTypeOfHours(Row row) {
        int position = row.getChildren().size() - 2;
        return (Autocomplete) row.getChildren().get(position);
    }

    /**
     * Locates {@link Intbox} Hours in {@link Row}
     * @param row
     * @return
     */
    private Intbox getIntboxHours(Row row) {
        int position = row.getChildren().size() - 3;
        return (Intbox) row.getChildren().get(position);
    }

    /**
     * Locates {@link Datebox} date in {@link Row}
     * @param row
     * @return
     */
    private Datebox getDateboxDate(Row row) {
        return (Datebox) row.getChildren().get(0);
    }

    /**
     * Locates {@link Textbox} Resource in {@link Row}
     * @param row
     * @return
     */
    private Autocomplete getTextboxResource(Row row) {
        return (Autocomplete) row.getChildren().get(1);
    }

    /**
     * Locates {@link Textbox} Order in {@link Row}
     *
     * @param row
     * @return
     */
    private Textbox getTextboxOrder(Row row) {
        return (Textbox) row.getChildren().get(2);
    }

    @Override
    public void goToList() {
        getVisibility().showOnly(listWindow);
        Util.reloadBindings(listWindow);
    }

    public void cancel() {
        if (workReportModel.isEditing()) {
            goToList();
        } else {
            workReportTypeCRUD.goToList();
        }
    }

    public void goToCreateForm(WorkReportType workReportType) {
        workReportModel.initCreate(workReportType);
        prepareWorkReportList();
        getVisibility().showOnly(createWindow);
        loadComponents(createWindow);
        Util.reloadBindings(createWindow);
    }

    public void goToEditForm(WorkReportDTO workReportDTO) {
        WorkReport workReport = workReportDTO.getWorkReport();
        goToEditForm(workReport);
    }

    public void goToEditForm(WorkReport workReport) {
        workReportModel.initEdit(workReport);
        loadComponents(createWindow);
        prepareWorkReportList();
        getVisibility().showOnly(createWindow);
        Util.reloadBindings(createWindow);
    }

    private void loadComponents(Component window) {
        listWorkReportLines = (NewDataSortableGrid) window
                .getFellow("listWorkReportLines");
        headingFieldsAndLabels = (Grid) window
                .getFellow("headingFieldsAndLabels");
        autocompleteResource = (Autocomplete) window
                .getFellow("autocompleteResource");
        txtOrderElement = (Textbox) window.getFellow("txtOrderElement");
    }

    /**
     * {@link WorkReportLine} list is finally constructed dynamically
     *
     * It seems there are some problems when a list of data is rendered,
     * modified (the data model changes), and it's rendered again. Deleting
     * previous settings and re-establishing the settings again each time the
     * list is rendered, solve those problems.
     *
     */
    private void prepareWorkReportList() {
        /*
         * The only way to clean the listhead, is to clean all its attributes
         * and children The paging component cannot be removed manually. It is
         * removed automatically when changing the mold
         */
         listWorkReportLines.setMold(null);
         listWorkReportLines.getChildren().clear();

         // Set mold and pagesize
         listWorkReportLines.setMold(MOLD);
         listWorkReportLines.setPageSize(PAGING);

         appendColumns(listWorkReportLines);
        listWorkReportLines
                .setSortedColumn((NewDataSortableColumn) listWorkReportLines
                        .getColumns().getFirstChild());

        listWorkReportLines.setModel(new SimpleListModel(getWorkReportLines()
                .toArray()));
    }

    /**
     * Appends list headers to {@link WorkReportLine} list
     *
     * @param listBox
     */
    private void appendColumns(Grid grid) {

        Columns columns = grid.getColumns();
        // Create listhead first time is rendered
        if (columns == null) {
            columns = new Columns();
        }
        // Delete all headers
        columns.getChildren().clear();
        columns.setSizable(true);

        // Add static headers
        if (getWorkReport() != null) {
            if (!getWorkReport().getWorkReportType().getDateIsSharedByLines()) {
                NewDataSortableColumn columnDate = new NewDataSortableColumn();
                columnDate.setLabel(_("Date"));
                columnDate.setSort("auto=(date)");
                columnDate.setSortDirection("ascending");

                columnDate.addEventListener("onSort", new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        sortWorkReportLines();
                    }
                });

                columnDate.setWidth("100px");
                columns.appendChild(columnDate);
            }
            if (!getWorkReport().getWorkReportType()
                    .getResourceIsSharedInLines()) {
                NewDataSortableColumn columnResource = new NewDataSortableColumn();
                columnResource.setLabel(_("Resource"));
                columnResource.setWidth("100px");
                columns.appendChild(columnResource);
            }
            if (!getWorkReport().getWorkReportType()
                    .getOrderElementIsSharedInLines()) {
                NewDataSortableColumn columnCode = new NewDataSortableColumn();
                columnCode.setLabel(_("Order Code"));
                columnCode.setWidth("100px");
                columns.appendChild(columnCode);
            }

            for (Object fieldOrLabel : workReportModel
                    .getFieldsAndLabelsLineByDefault()) {
                String columnName;
                if (fieldOrLabel instanceof DescriptionField) {
                    columnName = ((DescriptionField) fieldOrLabel)
                            .getFieldName();
                } else {
                    columnName = ((WorkReportLabelTypeAssigment) fieldOrLabel)
                            .getLabelType().getName();
                }
                NewDataSortableColumn columnFieldOrLabel = new NewDataSortableColumn();
                columnFieldOrLabel.setLabel(_(columnName));
                columnFieldOrLabel.setWidth("100px");
                columns.appendChild(columnFieldOrLabel);
            }

            if (!getWorkReport().getWorkReportType().getHoursManagement()
                    .equals(HoursManagementEnum.NUMBER_OF_HOURS)) {
                NewDataSortableColumn columnHourStart = new NewDataSortableColumn();
                columnHourStart.setLabel(_("Hour start"));
                columnHourStart.setWidth("50px");
                columns.appendChild(columnHourStart);
                NewDataSortableColumn columnHourFinish = new NewDataSortableColumn();
                columnHourFinish.setLabel(_("Hour finish"));
                columnHourFinish.setWidth("50px");
                columns.appendChild(columnHourFinish);
            }
        }
        NewDataSortableColumn columnNumHours = new NewDataSortableColumn();
        columnNumHours.setLabel(_("Hours"));
        columnNumHours.setWidth("50px");
        columns.appendChild(columnNumHours);
        NewDataSortableColumn columnHoursType = new NewDataSortableColumn();
        columnHoursType.setLabel(_("Hours type"));
        columnHoursType.setWidth("100px");
        columns.appendChild(columnHoursType);
        NewDataSortableColumn columnOperations = new NewDataSortableColumn();
        columnOperations.setLabel(_("Operations"));
        columnOperations.setAlign("center");
        columnOperations.setWidth("50px");
        columns.appendChild(columnOperations);

        columns.setParent(grid);

    }

    private WorkReportType getWorkReportType() {
        return getWorkReport().getWorkReportType();
    }

    public WorkReport getWorkReport() {
        return workReportModel.getWorkReport();
    }

    /**
     * Adds a new {@link WorkReportLine} to the list of rows
     *
     * @param rows
     */
    public void addWorkReportLine() {
        WorkReportLine workReportLine = workReportModel.addWorkReportLine();
        reloadWorkReportLines();
        // listWorkReportLines.getRows().appendChild(createWorkReportLine(workReportLine));
    }

    private void removeWorkReportLine(WorkReportLine workReportLine) {
        workReportModel.removeWorkReportLine(workReportLine);
        reloadWorkReportLines();
    }

    public List<WorkReportLine> getWorkReportLines() {
        return workReportModel.getWorkReportLines();
    }

    // /**
    // * Returns a new row bound to to a {@link WorkReportLine}
    // *
    // * A row consists of a several textboxes plus several listboxes, one
    // * for every {@link CriterionType} associated with current @{link
    // * WorkReport}
    // *
    // * @param workReportLine
    // * @return
    // */
    // private Row createWorkReportLine(WorkReportLine workReportLine) {
    // Row row = new Row();
    //
    // // Bind workReportLine to row
    // row.setValue(workReportLine);
    //
    // // Create textboxes
    // if (!getWorkReport().getWorkReportType().getDateIsSharedByLines()) {
    // appendDateInLines(row);
    // }
    // if (!getWorkReport().getWorkReportType().getResourceIsSharedInLines()) {
    // appendResourceInLines(row);
    // }
    // if (!getWorkReport().getWorkReportType()
    // .getOrderElementIsSharedInLines()) {
    // appendOrderElementInLines(row);
    // }
    //
    // // Create the fields and labels
    // appendFieldsAndLabelsInLines(row);
    //
    // if (!getWorkReport().getWorkReportType().getHoursManagement().equals(
    // HoursManagementEnum.NUMBER_OF_HOURS)) {
    // appendHourStart(row);
    // appendHourFinish(row);
    // }
    //
    // appendNumHours(row);
    // appendHoursType(row);
    // appendDeleteButton(row);
    //
    // return row;
    // }

    private void appendDateInLines(final Row row) {
        final Datebox date = new Datebox();
        date.setWidth("100px");
        final WorkReportLine line = (WorkReportLine) row.getValue();
        Util.bind(date, new Util.Getter<Date>() {

            @Override
            public Date get() {
                if (line != null) {
                    return line.getDate();
                }
                return null;
            }

        }, new Util.Setter<Date>() {

            @Override
            public void set(Date value) {
                if (line != null) {
                    line.setDate(value);
                }
            }
        });
        row.appendChild(date);
    }

    /**
     * Append a Autocomplete @{link Resource} to row
     *
     * @param row
     */
    private void appendResourceInLines(final Row row) {
        final Autocomplete autocomplete = new Autocomplete();
        autocomplete.setWidth("100px");
        autocomplete.setAutodrop(true);
        autocomplete.applyProperties();
        autocomplete.setFinder("ResourceFinder");

        // Getter, show worker selected
        if (getResource(row) != null) {
            autocomplete.setSelectedItem(getResource(row));
        }

        // Setter, set worker selected to WorkReportLine.resource
        autocomplete.addEventListener("onSelect", new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                final Comboitem comboitem = autocomplete.getSelectedItem();
                final WorkReportLine workReportLine = (WorkReportLine) row
                        .getValue();
                if ((comboitem == null)
                        || ((Resource) comboitem.getValue() == null)) {
                    workReportLine.setResource(null);
                    throw new WrongValueException(autocomplete,
                            _("Please, select an item"));
                } else {
                    workReportLine.setResource((Resource) comboitem.getValue());
                }
                reloadWorkReportLines();
            }
        });
        row.appendChild(autocomplete);
    }

    private Resource getResource(Row listitem) {
        WorkReportLine workReportLine = (WorkReportLine) listitem.getValue();
        return workReportLine.getResource();
    }

    /**
     * Append a Textbox @{link Order} to row
     *
     * @param row
     */
    private void appendOrderElementInLines(Row row) {
        Textbox txtOrder = new Textbox();
        txtOrder.setWidth("100px");
        bindTextboxOrder(txtOrder, (WorkReportLine) row.getValue());
        row.appendChild(txtOrder);
    }

    /**
     * Binds Textbox @{link Order} to a {@link WorkReportLine} {@link Order}
     *
     * @param txtOrder
     * @param workReportLine
     */
    private void bindTextboxOrder(final Textbox txtOrder,
            final WorkReportLine workReportLine) {
        Util.bind(txtOrder, new Util.Getter<String>() {

            @Override
            public String get() {
                if (workReportLine.getOrderElement() != null) {
                    try {
                        return workReportModel
                                .getDistinguishedCode(workReportLine
                                        .getOrderElement());
                    } catch (InstanceNotFoundException e) {
                    }
                }
                return "";
            }

        }, new Util.Setter<String>() {

            @Override
            public void set(String value) {
                if (value.length() > 0) {
                    try {
                        workReportLine.setOrderElement(workReportModel
                            .findOrderElement(value));
                    } catch (InstanceNotFoundException e) {
                        throw new WrongValueException(txtOrder,
                            _("OrderElement not found"));
                    }
                }
            }
        });
    }

    private void appendFieldsAndLabelsInLines(final Row row){
        final WorkReportLine line = (WorkReportLine)row.getValue();
        for(Object fieldOrLabel : getFieldsAndLabelsLine(line)){
            if(fieldOrLabel instanceof DescriptionValue){
                appendNewTextbox(row, (DescriptionValue) fieldOrLabel);
            } else if (fieldOrLabel instanceof Label) {
                appendAutocompleteLabelsByTypeInLine(row,
                        ((Label) fieldOrLabel));
            }
        }
    }

    private void appendAutocompleteLabelsByTypeInLine(Row row,
            final Label currentLabel) {
        final LabelType labelType = (LabelType) currentLabel.getType();
        final WorkReportLine line = (WorkReportLine) row.getValue();
        final Autocomplete comboLabels = createAutocompleteLabels(labelType,
                currentLabel);
        comboLabels.setParent(row);

        comboLabels.addEventListener(Events.ON_CHANGE, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (comboLabels.getSelectedItem() != null) {
                    Label newLabel = (Label) comboLabels.getSelectedItem()
                            .getValue();
                    workReportModel.changeLabelInWorkReportLine(currentLabel,
                            newLabel, line);
                }
                reloadWorkReportLines();
            }
        });
    }

    private void appendHourStart(final Row row) {
        final Timebox timeStart = new Timebox();
        timeStart.setWidth("50px");
        timeStart.setButtonVisible(true);
        final WorkReportLine line = (WorkReportLine) row.getValue();

        Util.bind(timeStart, new Util.Getter<Date>() {

            @Override
            public Date get() {
                if (line != null) {
                    return line.getClockStart();
                }
                return null;
            }

        }, new Util.Setter<Date>() {

            @Override
            public void set(Date value) {
                if (line != null) {
                    line.setClockStart(value);
                }
            }
        });

        timeStart.addEventListener(Events.ON_CHANGING, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                // force the binding
                Timebox timeFinish = (Timebox) getTimeboxFinish(row);
                timeFinish.setFocus(true);
                timeFinish.select();
                timeStart.setFocus(true);
                timeStart.select();
            }
        });

        timeStart.addEventListener(Events.ON_CHANGE, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                    reloadWorkReportLines();
            }
        });

        row.appendChild(timeStart);
    }

    private void appendHourFinish(final Row row) {
        final Timebox timeFinish = new Timebox();
        timeFinish.setWidth("50px");
        timeFinish.setButtonVisible(true);
        final WorkReportLine line = (WorkReportLine) row.getValue();

        Util.bind(timeFinish, new Util.Getter<Date>() {

            @Override
            public Date get() {
                if (line != null) {
                    return line.getClockFinish();
                }
                return null;
            }

        }, new Util.Setter<Date>() {

            @Override
            public void set(Date value) {
                if (line != null) {
                    line.setClockFinish(value);
                }
            }
        });

        timeFinish.addEventListener(Events.ON_CHANGING, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                Timebox timeStart = (Timebox) getTimeboxStart(row);
                timeStart.setFocus(true);
                timeStart.select();

                timeFinish.setFocus(true);
                timeFinish.select();
            }
        });

        timeFinish.addEventListener(Events.ON_CHANGE, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                    reloadWorkReportLines();
            }
        });

        row.appendChild(timeFinish);
    }

    /**
     * Append a {@link Intbox} numHours to {@link Row}
     *
     * @param row
     */
    private void appendNumHours(Row row) {
        Intbox intNumHours = new Intbox();
        intNumHours.setWidth("50px");
        WorkReportLine workReportLine = (WorkReportLine) row.getValue();
        bindIntboxNumHours(intNumHours, workReportLine);

        if (getWorkReportType().getHoursManagement().equals(
                HoursManagementEnum.HOURS_CALCULATED_BY_CLOCK)) {
            intNumHours.setReadonly(true);
        }
        row.appendChild(intNumHours);
    }

    private void appendHoursType(final Row row) {
        final WorkReportLine line = (WorkReportLine) row.getValue();
        final Autocomplete hoursType = new Autocomplete();
        hoursType.setWidth("100px");
        hoursType.setAutodrop(true);
        hoursType.applyProperties();
        hoursType.setFinder("TypeOfWorkHoursFinder");
        hoursType.setButtonVisible(true);

         if (line.getTypeOfWorkHours() != null) {
             hoursType.setSelectedItem(line.getTypeOfWorkHours());
         }

        hoursType.addEventListener("onSelect", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                final Comboitem comboitem = hoursType.getSelectedItem();
                if( (hoursType.getSelectedItem() == null) || ((TypeOfWorkHours) comboitem.getValue() == null)){
                    line.setTypeOfWorkHours(null);
                    throw new WrongValueException(hoursType,
                            _("Please, select an item"));
                } else {
                    line.setTypeOfWorkHours((TypeOfWorkHours) comboitem
                            .getValue());
                }
                reloadWorkReportLines();
            }
        });
        row.appendChild(hoursType);
    }

    /**
     * Append a delete {@link Button} to {@link Row}
     *
     * @param row
     */
    private void appendDeleteButton(final Row row) {
        Button delete = new Button("", "/common/img/ico_borrar1.png");
        delete.setHoverImage("/common/img/ico_borrar.png");
        delete.setSclass("icono");
        delete.setTooltiptext(_("Delete"));
        delete.addEventListener(Events.ON_CLICK, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                confirmRemove((WorkReportLine) row.getValue());
            }
        });
        row.appendChild(delete);
    }

    public void confirmRemove(WorkReportLine workReportLine) {
        try {
            int status = Messagebox.show(_("Confirm deleting {0}. Are you sure?", getWorkReportLineName(workReportLine)), _("Delete"),
                    Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION);
            if (Messagebox.OK == status) {
                removeWorkReportLine(workReportLine);
            }
        } catch (InterruptedException e) {
            messagesForUser.showMessage(
                    Level.ERROR, e.getMessage());
            LOG.error(_("Error on showing removing element: ", workReportLine.getId()), e);
        }
    }

    private String getWorkReportLineName(WorkReportLine workReportLine) {
        final Resource resource = (Resource) workReportLine.getResource();
        final OrderElement orderElement = workReportLine.getOrderElement();

        if (resource == null || orderElement == null) {
            return ITEM;
        }
        return resource.getShortDescription() + " - " + orderElement.getCode();
    }

    /**
     * Binds Intbox numHours to a {@link WorkReportLine} numHours
     * @param intNumHours
     * @param workReportLine
     */
    private void bindIntboxNumHours(final Intbox intNumHours,
            final WorkReportLine workReportLine) {
        Util.bind(intNumHours, new Util.Getter<Integer>() {

            @Override
            public Integer get() {
                return workReportLine.getNumHours();
            }

        }, new Util.Setter<Integer>() {

            @Override
            public void set(Integer value) {
                workReportLine.setNumHours(value);
            }
        });
    }

    public WorkReportListRenderer getRenderer() {
        return workReportListRenderer;
    }

    /**
     * RowRenderer for a @{WorkReportLine} element
     *
     * @author Diego Pino García <dpino@igalia.com>
     *
     */
    public class WorkReportListRenderer implements RowRenderer {

        @Override
        public void render(Row row, Object data) throws Exception {
            WorkReportLine workReportLine = (WorkReportLine) data;

            row.setValue(workReportLine);

            // Create textboxes
            if (!getWorkReport().getWorkReportType().getDateIsSharedByLines()) {
                appendDateInLines(row);
            }
            if (!getWorkReport().getWorkReportType()
                    .getResourceIsSharedInLines()) {
                appendResourceInLines(row);
            }
            if (!getWorkReport().getWorkReportType()
                    .getOrderElementIsSharedInLines()) {
                appendOrderElementInLines(row);
            }

            // Create the fields and labels
            appendFieldsAndLabelsInLines(row);

            if (!getWorkReport().getWorkReportType().getHoursManagement()
                    .equals(HoursManagementEnum.NUMBER_OF_HOURS)) {
                appendHourStart(row);
                appendHourFinish(row);
            }

            appendNumHours(row);
            appendHoursType(row);
            appendDeleteButton(row);
        }
    }

    /* Operations to manage the fields and labels in the heading */

    public String getCodeOrderElement() {
        if ((getWorkReport() != null)
                && (getWorkReport().getOrderElement() != null)) {
            try {
                return workReportModel.getDistinguishedCode(getWorkReport()
                        .getOrderElement());
            } catch (InstanceNotFoundException e) {
            }
        }
        return null;
    }

    public void setCodeOrderElement(String code) {
        if ((code != null) && (!code.isEmpty())) {
            try {
                getWorkReport().setOrderElement(
                        workReportModel.findOrderElement(code));
                reloadWorkReportLines();
            } catch (InstanceNotFoundException e) {
                throw new WrongValueException(txtOrderElement,
                        _("OrderElement not found"));
            }
        } else {
            getWorkReport().setOrderElement(null);
        }
    }

    public OrderedFieldsAndLabelsRowRenderer getOrderedFieldsAndLabelsRowRenderer() {
        return orderedFieldsAndLabelsRowRenderer;
    }

    public class OrderedFieldsAndLabelsRowRenderer implements RowRenderer {

        @Override
        public void render(Row row, Object data) throws Exception {
            row.setValue(data);

            if (data instanceof DescriptionValue) {
                appendNewLabel(row, ((DescriptionValue) data).getFieldName());
                appendNewTextbox(row, ((DescriptionValue) data));
            } else {
                appendNewLabel(row, ((Label) data).getType().getName());
                appendAutocompleteLabelsByType(row, ((Label) data));
            }
        }
    }

    private void appendNewLabel(Row row, String label) {
        org.zkoss.zul.Label labelName = new org.zkoss.zul.Label();
        labelName.setParent(row);
        labelName.setValue(label);
    }

    private void appendNewTextbox(Row row,
            final DescriptionValue descriptionValue) {
        Textbox textbox = new Textbox();
        Integer length = workReportModel.getLength(descriptionValue);
        textbox.setWidth(length.toString() + "px");
        textbox.setParent(row);

        Util.bind(textbox, new Util.Getter<String>() {

            @Override
            public String get() {
                if (descriptionValue != null) {
                    return descriptionValue.getValue();
                }
                return "";
            }

        }, new Util.Setter<String>() {

            @Override
            public void set(String value) {
                if (descriptionValue != null) {
                    descriptionValue.setValue(value);
                }
            }
        });
    }

    private void appendAutocompleteLabelsByType(Row row,
            final Label currentLabel) {
        final LabelType labelType = (LabelType) currentLabel.getType();
        final Autocomplete comboLabels = createAutocompleteLabels(labelType,
                currentLabel);
        comboLabels.setParent(row);

        comboLabels.addEventListener(Events.ON_CHANGE, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if(comboLabels.getSelectedItem() != null){
                    Label newLabel = (Label) comboLabels.getSelectedItem()
                            .getValue();
                    workReportModel.changeLabelInWorkReport(currentLabel,
                            newLabel);
                }
                Util.reloadBindings(headingFieldsAndLabels);
            }
        });
    }

    private Autocomplete createAutocompleteLabels(LabelType labelType,Label selectedLabel) {
        Autocomplete comboLabels = new Autocomplete();
        comboLabels.setButtonVisible(true);
        comboLabels.setWidth("100px");

        if (labelType != null) {
            final List<Label> listLabel = getMapLabelTypes()
                    .get(labelType);

            for (Label label : listLabel) {
                Comboitem comboItem = new Comboitem();
                comboItem.setValue(label);
                comboItem.setLabel(label.getName());
                comboItem.setParent(comboLabels);

                if ((selectedLabel != null)
                        && (selectedLabel.equals(label))) {
                    comboLabels.setSelectedItem(comboItem);
                }
            }
        }
        return comboLabels;
    }

    public List<Object> getFieldsAndLabelsHeading() {
        return workReportModel.getFieldsAndLabelsHeading();
    }

    public List<Object> getFieldsAndLabelsLine(WorkReportLine workReportLine) {
        return workReportModel.getFieldsAndLabelsLine(workReportLine);
    }

    private Map<LabelType, List<Label>> getMapLabelTypes() {
        return workReportModel.getMapAssignedLabelTypes();
    }

    public void changeResource(Comboitem selectedItem) {
        if (selectedItem != null) {
            getWorkReport().setResource((Resource) selectedItem.getValue());
        } else {
            getWorkReport().setResource(null);
        }
    }

    private void reloadWorkReportLines() {
        this.prepareWorkReportList();
        Util.reloadBindings(listWorkReportLines);
    }

    private void sortWorkReportLines() {
        System.out.println("sort lines");
        listWorkReportLines.setModel(new SimpleListModel(getWorkReportLines()
                .toArray()));
    }
}
