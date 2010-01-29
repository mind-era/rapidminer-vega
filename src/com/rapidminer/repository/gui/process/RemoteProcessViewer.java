package com.rapidminer.repository.gui.process;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import com.rapid_i.repository.wsimport.ProcessResponse;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RemoteProcessState;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.repository.gui.ToolTipProviderHelper;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.LogService;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

/**
 * 
 * @author Simon Fischer
 *
 */
public class RemoteProcessViewer extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;

	private Action STOP_ACTION = new ResourceAction(true, "remoteprocessviewer.stop") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			TreePath selectionPath = tree.getSelectionPath();
			if (selectionPath != null) {
				Object selection = selectionPath.getLastPathComponent();
				if (selection instanceof ProcessResponse) {
					ProcessResponse processResponse = (ProcessResponse)selection;
					if (!RemoteProcessState.valueOf(processResponse.getState()).isTerminated()) {
						if (selectionPath.getLastPathComponent() instanceof ProcessResponse) {
							RemoteRepository repository = (RemoteRepository) selectionPath.getPath()[1];
							try {

								if (!repository.getProcessService().stopProcess(processResponse.getId())) {
									SwingTools.showVerySimpleErrorMessage("remoteprocessviewer.stop_failed");	
								}
							} catch (RepositoryException e1) {
								SwingTools.showSimpleErrorMessage("remoteprocessviewer.stop_failed", e1);
							}			
						}
					}
				}
			}
		}
	};

	private Action SHOW_LOG_ACTION = new ResourceAction(true, "remoteprocessviewer.show_log") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			TreePath selectionPath = tree.getSelectionPath();
			if (selectionPath != null) {
				Object selection = selectionPath.getLastPathComponent();
				if (selection instanceof ProcessResponse) {					
					ProcessResponse processResponse = (ProcessResponse)selection;
					if (selection instanceof ProcessResponse) {
						RemoteRepository repository = (RemoteRepository) selectionPath.getPath()[1];
						repository.showLog(processResponse.getId());
					}
				}
			}
		}
	};

	private Action OPEN_ACTION = new ResourceAction(true, "remoteprocessviewer.open") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			TreePath selectionPath = tree.getSelectionPath();
			if (selectionPath != null) {
				Object selection = selectionPath.getLastPathComponent();
				if (selection instanceof ProcessResponse) {
					Repository repository = (Repository) selectionPath.getPath()[1];
					RepositoryLocation loc = new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX+repository.getName()+
							((ProcessResponse) selection).getProcessLocation());
					OpenAction.open(new RepositoryProcessLocation(loc), true);							
				} else if (selection instanceof OutputLocation) {
					try {
						Repository repository = (Repository) selectionPath.getPath()[1];
						ProcessResponse proResponse = (ProcessResponse) selectionPath.getPath()[2];
						RepositoryLocation procLoc = new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX+repository.getName()+
								proResponse.getProcessLocation());
						RepositoryLocation ioLoc = new RepositoryLocation(procLoc.parent(), ((OutputLocation)selection).getLocation());

						RepositoryTree.showAsResult((IOObjectEntry)ioLoc.locateEntry());
					} catch (Exception e1) {
						SwingTools.showSimpleErrorMessage("cannot_fetch_data_from_repository", e1);
					}					
				}
			}					
		}
	};

	private Action BROWSE_ACTION = new ResourceAction(true, "remoteprocessviewer.browse") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			TreePath selectionPath = tree.getSelectionPath();
			if (selectionPath != null) {
				Object selection = selectionPath.getLastPathComponent();
				if (selection instanceof ProcessResponse) {
					RemoteRepository repository = (RemoteRepository) selectionPath.getPath()[1];
					repository.browse(((ProcessResponse) selection).getProcessLocation());
				} else if (selection instanceof OutputLocation) {
					try {
						RemoteRepository repository = (RemoteRepository) selectionPath.getPath()[1];
						ProcessResponse proResponse = (ProcessResponse) selectionPath.getPath()[2];
						RepositoryLocation procLoc = new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX+repository.getName()+
								proResponse.getProcessLocation());
						RepositoryLocation ioLoc = new RepositoryLocation(procLoc.parent(), ((OutputLocation)selection).getLocation());
						repository.browse(ioLoc.getPath());

					} catch (Exception e1) {
						SwingTools.showSimpleErrorMessage("cannot_fetch_data_from_repository", e1);
					}					
				}
			}					
		}
	};

	public RemoteProcessViewer() {
		setLayout(new BorderLayout());
		tree = new JTree(new RemoteProcessesTreeModel());
		tree.setCellRenderer(new RemoteProcessTreeCellRenderer());
		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		add(tree, BorderLayout.CENTER);

		JToolBar toolBar = new ViewToolBar();
		add(toolBar, BorderLayout.NORTH);
		toolBar.add(OPEN_ACTION);
		toolBar.add(BROWSE_ACTION);
		toolBar.add(STOP_ACTION);
		toolBar.add(SHOW_LOG_ACTION);

		new ToolTipWindow(new TipProvider() {
			@Override
			public Component getCustomComponent(Object id) {
				if (id instanceof TreePath) {
					RepositoryLocation loc = getSelectedRepositoryLocation((TreePath) id);
					if (loc != null) {
						try {
							return ToolTipProviderHelper.getCustomComponent(loc.locateEntry());
						} catch (RepositoryException e) {
							LogService.getRoot().log(Level.WARNING, "Error locating entry for "+loc+": "+e, e);
							return null;
						}
					} else {
						return null;
					}
				} else {
					return null;
				}
			}

			@Override
			public Object getIdUnder(Point point) {
				TreePath path = tree.getPathForLocation((int)point.getX(), (int)point.getY());
				if (path != null) {
					return path;
				} else {
					return null;
				}
			}

			@Override
			public String getTip(Object o) {
				if (o instanceof TreePath) {
					Object last = ((TreePath) o).getLastPathComponent();
					if (last instanceof ProcessResponse) {
						ProcessResponse pr = (ProcessResponse) last;
						StringBuilder b = new StringBuilder();
						b.append("<html><body>");
						b.append("<strong>").append(pr.getProcessLocation()).append("</strong> ");
						if (RemoteProcessState.valueOf(pr.getState()) == RemoteProcessState.FAILED) {
							b.append("<span style=\"color:red\">(").append(pr.getState().toLowerCase()).append(")</span><br/>");
						} else {
							b.append("(").append(pr.getState().toLowerCase()).append(")<br/>");
						}
						if (pr.getStartTime() != null) {
							b.append("<em>Started: </em>").append(DateFormat.getDateTimeInstance().format(pr.getStartTime().toGregorianCalendar().getTime())).append("<br/>");
						}
						if (pr.getCompletionTime() != null) {
							b.append("<em>Completed: </em>").append(DateFormat.getDateTimeInstance().format(pr.getCompletionTime().toGregorianCalendar().getTime())).append("<br/>");
						}
						if (pr.getException() != null) {
							b.append("<span style=\"color:red\">").append(pr.getException()).append("</span><br/>");
						}
						RemoteRepository repos = (RemoteRepository) ((TreePath)o).getPath()[1];
						b.append("<a href=\""+repos.getProcessLogURI(pr.getId()).toString()+"\">View Log</a>");
						b.append("</body></html>");
						return b.toString();
					} else {
						RepositoryLocation loc = getSelectedRepositoryLocation((TreePath)o);
						if (loc != null) {
							try {
								return ToolTipProviderHelper.getTip(loc.locateEntry());
							} catch (RepositoryException e) {
								LogService.getRoot().log(Level.WARNING, "Error locating entry for "+loc+": "+e, e);
								return null;
							}
						} else {
							return null;
						}
					}
				} else {
					return null;
				}
			}
		}, tree);
		//		tree.addMouseListener(new MouseAdapter() {
		//			@Override
		//			public void mouseClicked(MouseEvent e) {
		//				if (e.getClickCount() == 2) {
		//				}
		//			}
		//		});
	}

	private RepositoryLocation getSelectedRepositoryLocation(TreePath selectionPath) {
		//TreePath selectionPath = tree.getSelectionPath();
		if (selectionPath != null) {
			Object selection = selectionPath.getLastPathComponent();
			if (selection instanceof ProcessResponse) {
				Repository repository = (Repository) selectionPath.getPath()[1];
				return new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX+repository.getName()+
						((ProcessResponse) selection).getProcessLocation());						
			} else if (selection instanceof OutputLocation) {
				Repository repository = (Repository) selectionPath.getPath()[1];
				ProcessResponse proResponse = (ProcessResponse) selectionPath.getPath()[2];
				RepositoryLocation procLoc = new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX+repository.getName()+
						proResponse.getProcessLocation());
				return new RepositoryLocation(procLoc.parent(), ((OutputLocation)selection).getLocation());
			}
		}
		return null;
	}

	public static final String PROCESS_PANEL_DOCK_KEY = "remote_process_viewer";
	private final DockKey DOCK_KEY = new ResourceDockKey(PROCESS_PANEL_DOCK_KEY);

	private JTree tree;
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}
}