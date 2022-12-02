/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.gui;

import biz.ganttproject.core.chart.render.ShapeConstants;
import biz.ganttproject.core.chart.render.ShapePaint;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import com.google.common.base.Objects;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIUtil.DateValidator;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.SpringUtilities;
import net.sourceforge.ganttproject.gui.taskproperties.CustomColumnsPanel;
import net.sourceforge.ganttproject.gui.taskproperties.TaskAllocationsPanel;
import net.sourceforge.ganttproject.gui.taskproperties.TaskDependenciesPanel;
import net.sourceforge.ganttproject.gui.taskproperties.TaskScheduleDatesPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.shape.JPaintCombo;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.util.BrowserControl;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Real panel for editing task properties
 */
public class GanttMultTaskPropertiesBean extends JPanel {

  private ColorOption myTaskColorOption = new DefaultColorOption("");
  private final GPAction mySetDefaultColorAction = new GPAction("defaultColor") {
    @Override
    public void actionPerformed(ActionEvent e) {
      myTaskColorOption.setValue(myUIfacade.getGanttChart().getTaskDefaultColorOption().getValue());
    }
  };
  private JXDatePicker myEarliestBeginDatePicker;

  private GanttTask[] selectedTasks;

  private static final GanttLanguage language = GanttLanguage.getInstance();

  private GanttCalendar myThird;

  private JTabbedPane tabbedPane; // TabbedPane that includes the following four
                                  // items

  private JPanel generalPanel;

  private JComponent predecessorsPanel;

  private JPanel resourcesPanel;

  private JPanel notesPanel;

  private JTextField nameField1;

  private JTextField tfWebLink;

  private JButton bWebLink;

  private JSpinner percentCompleteSlider;

  private JComboBox priorityComboBox;

  private JCheckBox myEarliestBeginEnabled;

  private JCheckBox mileStoneCheckBox1;

  private JCheckBox projectTaskCheckBox1;

  /** Shape chooser combo Box */
  private JPaintCombo shapeComboBox;

  private JScrollPane scrollPaneNotes;

  private JTextArea noteAreaNotes;

  private JPanel secondRowPanelNotes;

  private String originalName;

  private String originalWebLink;

  private boolean originalIsMilestone;

  private GanttCalendar originalStartDate;

  private GanttCalendar originalEndDate;

  private GanttCalendar originalEarliestBeginDate;

  private int originalEarliestBeginEnabled;

  private boolean originalIsProjectTask;

  private String originalNotes;

  private int originalCompletionPercentage;

  private Task.Priority originalPriority;

  private ShapePaint originalShape;

  private final TaskScheduleDatesPanel myTaskScheduleDates;

  private CustomColumnsPanel myCustomColumnPanel = null;

  private TaskDependenciesPanel myDependenciesPanel;

  private TaskAllocationsPanel myAllocationsPanel;

  private final HumanResourceManager myHumanResourceManager;

  private final RoleManager myRoleManager;

  private Task myUnpluggedClone;
  private final TaskManager myTaskManager;
  private final IGanttProject myProject;
  private final UIFacade myUIfacade;

  private JCheckBox myShowInTimeline;
  private AbstractAction myOnEarliestBeginToggle;

  public GanttMultTaskPropertiesBean(GanttTask[] selectedTasks, IGanttProject project, UIFacade uifacade) {
    myTaskScheduleDates = new TaskScheduleDatesPanel(uifacade);
    this.selectedTasks = selectedTasks;
    storeOriginalValues(selectedTasks[0]);
    myHumanResourceManager = project.getHumanResourceManager();
    myRoleManager = project.getRoleManager();
    myTaskManager = project.getTaskManager();
    myProject = project;
    myUIfacade = uifacade;
    init();
    setSelectedTaskProperties();
  }

  private static void addEmptyRow(JPanel form) {
    form.add(Box.createRigidArea(new Dimension(1, 10)));
    form.add(Box.createRigidArea(new Dimension(1, 10)));
  }

  /** Construct the general panel */
  private void constructGeneralPanel() {
    final JPanel propertiesPanel = new JPanel(new SpringLayout());

    propertiesPanel.add(new JLabel(language.getText("name")));
    nameField1 = new JTextField(20);
    nameField1.setName("name_of_task");
    propertiesPanel.add(nameField1);
    addEmptyRow(propertiesPanel);

    myTaskScheduleDates.insertInto(propertiesPanel);

    constructEarliestBegin(propertiesPanel);
    addEmptyRow(propertiesPanel);

    propertiesPanel.add(new JLabel(language.getText("priority")));
    priorityComboBox = new JComboBox();
    for (Task.Priority p : Task.Priority.values()) {
      priorityComboBox.addItem(language.getText(p.getI18nKey()));
    }
    priorityComboBox.setEditable(false);
    propertiesPanel.add(priorityComboBox);

    propertiesPanel.add(new JLabel(language.getText("advancement")));
    SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, 100, 1);
    percentCompleteSlider = new JSpinner(spinnerModel);
    propertiesPanel.add(percentCompleteSlider);

    addEmptyRow(propertiesPanel);

    OptionsPageBuilder builder = new OptionsPageBuilder(GanttTaskPropertiesBean.this, OptionsPageBuilder.TWO_COLUMN_LAYOUT);
    builder.setUiFacade(myUIfacade);
    JPanel colorBox = new JPanel(new BorderLayout(5, 0));
    colorBox.add(builder.createColorComponent(myTaskColorOption).getJComponent(), BorderLayout.WEST);
    //colorBox.add(Box.createHorizontalStrut(5));
    colorBox.add(new JXHyperlink(mySetDefaultColorAction), BorderLayout.CENTER);
    //colorBox.add(Box.createHorizontalGlue());
    //colorBox.add(Box.createHorizontalGlue());
    //colorBox.add(Box.createHorizontalGlue());

    propertiesPanel.add(new JLabel(language.getText("colors")));
    propertiesPanel.add(colorBox);

    SpringUtilities.makeCompactGrid(propertiesPanel, propertiesPanel.getComponentCount() / 2, 2, 1, 1, 5, 5);

    JPanel propertiesWrapper = new JPanel(new BorderLayout());
    propertiesWrapper.add(propertiesPanel, BorderLayout.NORTH);
    generalPanel = new JPanel(new SpringLayout());
    //generalPanel.add(new JLayer<JPanel>(propertiesPanel, layerUi));
    generalPanel.add(propertiesWrapper);
    generalPanel.add(notesPanel);
    SpringUtilities.makeCompactGrid(generalPanel, 1, 2, 1, 1, 10, 5);
  }

  private void constructEarliestBegin(Container propertiesPanel) {
    final JXHyperlink copyFromBeginDate = new JXHyperlink(new GPAction("option.taskProperties.main.earliestBegin.copyBeginDate") {
      @Override
      public void actionPerformed(ActionEvent e) {
        setThird(myTaskScheduleDates.getStart());
      }

      @Override
      protected String getLocalizedName() {
        String fallbackLabel = String.format("%s %s", language.getText("copy"), language.getText("generic.startDate.label"));
        return Objects.firstNonNull(super.getLocalizedName(), fallbackLabel);
      }

    });
    myEarliestBeginDatePicker = UIUtil.createDatePicker();
    Box valueBox = Box.createHorizontalBox();
    myOnEarliestBeginToggle = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myEarliestBeginDatePicker.setEnabled(myEarliestBeginEnabled.isSelected());
        if (getThird() == null) {
          setThird(myTaskScheduleDates.getStart());
        }
        copyFromBeginDate.setEnabled(myEarliestBeginEnabled.isSelected());
      }
    };
    myEarliestBeginEnabled = new JCheckBox(myOnEarliestBeginToggle);
    valueBox.add(myEarliestBeginEnabled);
    valueBox.add(Box.createHorizontalStrut(10));
    valueBox.add(myEarliestBeginDatePicker);
    valueBox.add(Box.createHorizontalStrut(5));
    valueBox.add(copyFromBeginDate);
    propertiesPanel.add(new JLabel(language.getText("earliestBegin")));
    propertiesPanel.add(valueBox);
  }

  /** Initialize the widgets */
  private void init() {

    tabbedPane = new JTabbedPane() {
      @Override
      public void addTab(String title, Icon icon, Component component) {
        super.addTab(title, icon, UIUtil.contentPaneBorder((JComponent)component));
      }
    };
    constructGeneralPanel();

    tabbedPane.addTab(language.getText("general"), new ImageIcon(getClass().getResource("/icons/properties_16.gif")),
        generalPanel);

    setLayout(new BorderLayout());

    add(tabbedPane, BorderLayout.CENTER);

    tabbedPane.addFocusListener(new FocusAdapter() {
      private boolean isFirstFocusGain = true;

      @Override
      public void focusGained(FocusEvent e) {
        super.focusGained(e);
        if (isFirstFocusGain) {
          nameField1.requestFocus();
          isFirstFocusGain = false;
        }
      }
    });
    tabbedPane.setBorder(BorderFactory.createEmptyBorder(2, 0, 5, 0));
  }

  /** Apply the modified properties to the selected Tasks */

  private void setSelectedTaskProperties() {
    myUnpluggedClone = selectedTasks[0].unpluggedClone();
    nameField1.setText(originalName);

    setName(selectedTasks[0].toString());

    percentCompleteSlider.setValue(new Integer(originalCompletionPercentage));
    priorityComboBox.setSelectedIndex(originalPriority.ordinal());

    myTaskScheduleDates.setUnpluggedClone(myUnpluggedClone);
    DateValidator validator = UIUtil.DateValidator.Default.aroundProjectStart(myProject.getTaskManager().getProjectStart());
    ActionListener listener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setThird(CalendarFactory.createGanttCalendar(((JXDatePicker) e.getSource()).getDate()));
      }
    };
    UIUtil.setupDatePicker(myEarliestBeginDatePicker, originalEarliestBeginDate == null ? null : originalEarliestBeginDate.getTime(), validator, listener);
    myThird = originalEarliestBeginDate;
    myEarliestBeginEnabled.setSelected(originalEarliestBeginEnabled == 1);
    myOnEarliestBeginToggle.actionPerformed(null);


    if (mileStoneCheckBox1 != null) {
      mileStoneCheckBox1.setSelected(originalIsMilestone);
    } else if (projectTaskCheckBox1 != null) {
      projectTaskCheckBox1.setSelected(originalIsProjectTask);
    }
    myTaskScheduleDates.setupFields(isMilestone(), isSupertask());

    tfWebLink.setText(originalWebLink);

    if (selectedTasks[0].shapeDefined()) {
      for (int j = 0; j < ShapeConstants.PATTERN_LIST.length; j++) {
        if (originalShape.equals(ShapeConstants.PATTERN_LIST[j])) {
          shapeComboBox.setSelectedIndex(j);
          break;
        }
      }
    }

    noteAreaNotes.setText(originalNotes);
    myTaskColorOption.setValue(selectedTasks[0].getColor());
    myShowInTimeline.setSelected(myUIfacade.getCurrentTaskView().getTimelineTasks().contains(selectedTasks[0]));
  }

  private boolean isSupertask() {
    return myUnpluggedClone.getManager().getTaskHierarchy().hasNestedTasks(selectedTasks[0]);
  }


  private boolean isMilestone() {
    if (mileStoneCheckBox1 == null) {
      return false;
    }
    return mileStoneCheckBox1.isSelected();
  }



  private boolean isProjectTask() {
    return projectTaskCheckBox1.isSelected();
  }

  private int getThirdDateConstraint() {
    return myEarliestBeginEnabled.isSelected() ? 1 : 0;
  }

  private String getNotes() {
    return noteAreaNotes.getText();
  }

  private String getTaskName() {
    String text = nameField1.getText();
    return text == null ? "" : text.trim();
  }

  private String getWebLink() {
    String text = tfWebLink.getText();
    return text == null ? "" : text.trim();
  }

  private int getPercentComplete() {
    return ((Integer) percentCompleteSlider.getValue()).hashCode();
  }

  private Task.Priority getPriority() {
    return Task.Priority.getPriority(priorityComboBox.getSelectedIndex());
  }

  public GanttCalendar getStart() {
    return myTaskScheduleDates.getStart();
  }

  public GanttCalendar getEnd() {
    return myTaskScheduleDates.getEnd();
  }

  private int getLength() {
    return myTaskScheduleDates.getLength();
  }

  private GanttCalendar getThird() {
    return myThird;
  }



  private void setThird(GanttCalendar third) {
    myThird = third;
    myEarliestBeginDatePicker.setDate(myThird.getTime());
  }

  private boolean canBeProjectTask(Task testedTask, TaskContainmentHierarchyFacade taskHierarchy) {
    Task[] nestedTasks = taskHierarchy.getNestedTasks(testedTask);
    if (nestedTasks.length == 0) {
      return false;
    }
    for (Task parent = taskHierarchy.getContainer(testedTask); parent != null; parent = taskHierarchy.getContainer(parent)) {
      if (parent.isProjectTask()) {
        return false;
      }
    }
    for (Task nestedTask : nestedTasks) {
      if (isProjectTaskOrContainsProjectTask(nestedTask)) {
        return false;
      }
    }
    return true;
  }

  private boolean isProjectTaskOrContainsProjectTask(Task task) {
    if (task.isProjectTask()) {
      return true;
    }
    for (Task nestedTask : task.getNestedTasks()) {
      if (isProjectTaskOrContainsProjectTask(nestedTask)) {
        return true;
      }
    }
    return false;
  }

}
