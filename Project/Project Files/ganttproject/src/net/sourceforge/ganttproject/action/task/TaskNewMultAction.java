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
package net.sourceforge.ganttproject.action.task;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.gui.GanttMultTaskPropertiesBean;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import net.sourceforge.ganttproject.GanttTask;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import javax.swing.Action;

public class TaskNewMultAction extends GPAction {
  private final IGanttProject myProject;
  private final UIFacade myUiFacade;
  private final Calendar myCalendar;


  public TaskNewMultAction(IGanttProject project, UIFacade uiFacade) {
    this(project, uiFacade, IconSize.MENU);
  }

  private TaskNewMultAction(IGanttProject project, UIFacade uiFacade, IconSize size) {
    super("task.newMult", size.asString());
    myProject = project;
    myUiFacade = uiFacade;
    myCalendar = Calendar.getInstance();
  }

  @Override
  public GPAction withIcon(IconSize size) {
    return new TaskNewMultAction(myProject, myUiFacade, size);
  }

  @Override
  public void actionPerformed(ActionEvent arg){
    show(myProject, myUiFacade);
  }

  public void show(final IGanttProject project, final UIFacade uiFacade) {
    List<Task> selection = uiFacade.getTaskSelectionManager().getSelectedTasks();
    final GanttTask[] tasks = new GanttTask[] { (GanttTask) selection.get(0) };
    final GanttLanguage language = GanttLanguage.getInstance();
    final GanttMultTaskPropertiesBean taskPropertiesBean = new GanttMultTaskPropertiesBean(tasks, project, uiFacade);
    final Action[] actions = new Action[] { new OkAction() {
      public void actionPerformed(ActionEvent arg0) {
        uiFacade.getUndoManager().undoableEdit(language.getText("properties.changed"), new Runnable() {
          public void run() {
            try {
              project.getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
            } catch (TaskDependencyException e) {
              if (!GPLogger.log(e)) {
                e.printStackTrace();
              }
            }
            List<Task> selection = myUiFacade.getTaskSelectionManager().getSelectedTasks();
            Date i = taskPropertiesBean.getStart().getTime();
            myCalendar.setTime(i);
            while(i.before(taskPropertiesBean.getEnd().getTime())){
              Task selectedTask = selection.isEmpty() ? null : (GanttTask)selection.get(0);
              Task newTask = getTaskManager().newTaskBuilder()
                      .withPrevSibling(selectedTask).withStartDate(i).build();
              myUiFacade.getTaskTree().startDefaultEditing(newTask);
              myCalendar.add(Calendar.DATE, 1);
              i = myCalendar.getTime();
            }
            uiFacade.refresh();
          }
        });
      }
    }, CancelAction.EMPTY };
  }

  protected TaskManager getTaskManager() {
    return myProject.getTaskManager();
  }

  protected UIFacade getUIFacade() {
    return myUiFacade;
  }

  @Override
  public void updateAction() {
    super.updateAction();
  }

  @Override
  public TaskNewMultAction asToolbarAction() {
    TaskNewMultAction result = new TaskNewMultAction(myProject, myUiFacade);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    return result;
  }
}