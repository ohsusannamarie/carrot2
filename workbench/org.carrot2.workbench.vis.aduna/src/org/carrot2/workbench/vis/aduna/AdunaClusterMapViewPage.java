package org.carrot2.workbench.vis.aduna;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.*;

import org.apache.commons.lang.StringUtils;
import org.carrot2.core.*;
import org.carrot2.core.Cluster;
import org.carrot2.workbench.core.helpers.PostponableJob;
import org.carrot2.workbench.core.ui.SearchEditor;
import org.carrot2.workbench.core.ui.SearchResultListenerAdapter;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.progress.UIJob;

import com.google.common.collect.Maps;

import biz.aduna.map.cluster.*;

/**
 * A single {@link AdunaClusterMapViewPage} page embeds Aduna's Swing component with
 * visualization of clusters.
 */
final class AdunaClusterMapViewPage extends Page
{
    /*
     * 
     */
    private final int REFRESH_DELAY = 500;

    /**
     * Visualization refresh job. Postponed a bit to make the user interface more
     * responsive.
     */
    private PostponableJob refreshJob = new PostponableJob(
        new UIJob("Aduna (refresh)...")
        {
            private Map<Integer, Classification> clusterMap;
            private Map<Integer, DefaultObject> documentMap;

            @SuppressWarnings("unchecked")
            public IStatus runInUIThread(IProgressMonitor monitor)
            {
                final ProcessingResult result = editor.getSearchResult()
                    .getProcessingResult();
                if (result != null)
                {
                    final DefaultClassification root = new DefaultClassification("All clusters");
                    updateMaps(result);
                    toClassification(root, result.getClusters());
                    mapMediator.setClassificationTree(root);
                    mapMediator.visualize(new ArrayList(root.getChildren()));
                }
                return Status.OK_STATUS;
            }

            private void updateMaps(ProcessingResult result)
            {
                clusterMap = Maps.newHashMap();
                documentMap = Maps.newHashMap();
            }

            private void toClassification(DefaultClassification parent, java.util.List<Cluster> clusters)
            {
                for (Cluster cluster : clusters)
                {
                    if (clusterMap.containsKey(cluster.getId()))
                        continue;

                    final DefaultClassification cc = new DefaultClassification(cluster.getLabel(), parent);
                    clusterMap.put(cluster.getId(), cc);

                    for (Document d : cluster.getAllDocuments())
                    {
                        if (!documentMap.containsKey(d.getId()))
                        {
                            String dt = (String) (String) d.getField(Document.TITLE);
                            String title = "[" + d.getId() + "]";
                            if (!StringUtils.isEmpty(dt))
                            {
                                title = title + " " + dt;
                            }

                            documentMap.put(d.getId(), new DefaultObject(title));
                        }

                        cc.add(documentMap.get(d.getId()));
                    }

                    toClassification(cc, cluster.getSubclusters());
                }
            }
        });

    /*
     * Sync with search result updated event.
     */
    private final SearchResultListenerAdapter editorSyncListener = new SearchResultListenerAdapter()
    {
        public void processingResultUpdated(ProcessingResult result)
        {
            refreshJob.reschedule(REFRESH_DELAY);
        }
    };

    /**
     * Editor selection listener.
     */
    private final ISelectionChangedListener selectionListener = new ISelectionChangedListener()
    {
        /* */
        public void selectionChanged(SelectionChangedEvent event)
        {
            final ISelection selection = event.getSelection();
            if (selection != null && selection instanceof IStructuredSelection)
            {
                final IStructuredSelection ss = (IStructuredSelection) selection;
            }
        }
    };

    /*
     * 
     */
    private SearchEditor editor;

    /**
     * SWT's composite inside which Aduna is embedded (AWT/Swing).
     */
    private Composite embedder;

    /**
     * Aduna's GUI mediator component.
     */
    private ClusterMapMediator mapMediator;

    /*
     * 
     */
    public AdunaClusterMapViewPage(SearchEditor editor)
    {
        this.editor = editor;
    }

    /*
     * 
     */
    @Override
    public void createControl(Composite parent)
    {
        embedder = createAdunaControl(parent);

        /*
         * Add a listener to the editor to update the view after new clusters are
         * available.
         */
        if (editor.getSearchResult().getProcessingResult() != null)
        {
            refreshJob.reschedule(REFRESH_DELAY);
        }

        editor.getSearchResult().addListener(editorSyncListener);
        editor.addPostSelectionChangedListener(selectionListener);
    }

    /*
     * 
     */
    @SuppressWarnings("serial")
    private Composite createAdunaControl(Composite parent)
    {
        /*
         * If <code>true</code>, try some dirty hacks to avoid flicker on Windows.
         */
        final boolean windowsFlickerHack = true;
        if (windowsFlickerHack)
        {
            System.setProperty("sun.awt.noerasebackground", "true");
        }

        final ScrolledComposite scroll = new ScrolledComposite(parent, SWT.H_SCROLL
            | SWT.V_SCROLL);
        scroll.setAlwaysShowScrollBars(true);

        final Composite embedded = new Composite(scroll, SWT.EMBEDDED);
        scroll.setContent(embedded);

        final Color swtBackground = parent.getDisplay().getSystemColor(SWT.COLOR_GRAY);
        final java.awt.Color awtBackground = new java.awt.Color(swtBackground.getRed(),
            swtBackground.getGreen(), swtBackground.getBlue());
        scroll.setBackground(swtBackground);

        final Frame frame = SWT_AWT.new_Frame(embedded);
        frame.setLayout(new BorderLayout());

        final Panel frameRootPanel = new Panel(new BorderLayout())
        {
            public void update(java.awt.Graphics g)
            {
                if (windowsFlickerHack)
                {
                    paint(g);
                }
                else
                {
                    super.update(g);
                }
            }
        };
        frame.add(frameRootPanel);

        final JRootPane rootPane = new JRootPane();
        frameRootPanel.add(rootPane);

        /*
         * We embed ClusterMap inside a JScrollPane that never shows scrollbars because we
         * want our scrollbars to be drawn by SWT components.
         */
        final JScrollPane scrollPanel = new JScrollPane(
            JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.setBackground(awtBackground);
        scrollPanel.setBorder(BorderFactory.createLineBorder(awtBackground));
        rootPane.getContentPane().add(scrollPanel, BorderLayout.CENTER);

        final ClusterMapFactory factory = ClusterMapFactory.createFactory();
        final ClusterMap clusterMap = factory.createClusterMap();
        final ClusterMapMediator mapMediator = factory.createMediator(clusterMap);

        this.mapMediator = mapMediator;

        final ClusterGraphPanel graphPanel = mapMediator.getGraphPanel();
        scrollPanel.setViewportView(graphPanel);

        graphPanel.addComponentListener(new ComponentAdapter()
        {
            public void componentResized(final ComponentEvent e)
            {
                embedded.getDisplay().asyncExec(new Runnable()
                {
                    public void run()
                    {
                        final Dimension preferredSize = e.getComponent()
                            .getPreferredSize();
                        final Rectangle clientArea = scroll.getClientArea();

                        embedded.setSize(Math.max(preferredSize.width, clientArea.width),
                            Math.max(preferredSize.height, clientArea.height));
                    }
                });
            }
        });

        scroll.addControlListener(new ControlAdapter()
        {
            public void controlResized(ControlEvent e)
            {
                // This is not thread-safe here, is it?
                final Dimension preferredSize = graphPanel.getPreferredSize();
                final Rectangle clientArea = scroll.getClientArea();

                embedded.setSize(Math.max(preferredSize.width, clientArea.width), Math
                    .max(preferredSize.height, clientArea.height));
            }
        });

        return scroll;
    }

    /*
     * 
     */
    @Override
    public Control getControl()
    {
        return embedder;
    }

    /*
     * 
     */
    @Override
    public void dispose()
    {
        editor.getSearchResult().removeListener(editorSyncListener);
        editor.removePostSelectionChangedListener(selectionListener);

        embedder.dispose();

        super.dispose();
    }

    /*
     * 
     */
    @Override
    public void setFocus()
    {
        // Ignore.
    }
}
