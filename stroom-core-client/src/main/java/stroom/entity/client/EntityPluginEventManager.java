/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.entity.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import stroom.alert.client.event.ConfirmEvent;
import stroom.alert.client.presenter.ConfirmCallback;
import stroom.content.client.event.ContentTabSelectionChangeEvent;
import stroom.core.client.KeyboardInterceptor;
import stroom.core.client.KeyboardInterceptor.KeyTest;
import stroom.core.client.MenuKeys;
import stroom.core.client.presenter.Plugin;
import stroom.dispatch.client.AsyncCallbackAdaptor;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.entity.client.event.CopyEntityEvent;
import stroom.entity.client.event.CreateEntityEvent;
import stroom.entity.client.event.MoveEntityEvent;
import stroom.entity.client.event.ReloadEntityEvent;
import stroom.entity.client.event.RenameEntityEvent;
import stroom.entity.client.event.SaveAsEntityEvent;
import stroom.entity.client.event.SaveEntityEvent;
import stroom.entity.client.event.ShowCopyEntityDialogEvent;
import stroom.entity.client.event.ShowCreateEntityDialogEvent;
import stroom.entity.client.event.ShowMoveEntityDialogEvent;
import stroom.entity.client.event.ShowPermissionsEntityDialogEvent;
import stroom.entity.client.event.ShowRenameEntityDialogEvent;
import stroom.entity.client.event.ShowSaveAsEntityDialogEvent;
import stroom.entity.client.presenter.EntityEditPresenter;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.DocumentType;
import stroom.entity.shared.Folder;
import stroom.explorer.client.event.ExplorerTreeDeleteEvent;
import stroom.explorer.client.event.ExplorerTreeSelectEvent;
import stroom.explorer.client.event.HighlightExplorerItemEvent;
import stroom.explorer.client.event.RefreshExplorerTreeEvent;
import stroom.explorer.client.event.ShowExplorerMenuEvent;
import stroom.explorer.client.event.ShowNewMenuEvent;
import stroom.explorer.client.presenter.DocumentTypeCache;
import stroom.explorer.client.presenter.MultiSelectionModel;
import stroom.explorer.shared.DocumentTypes;
import stroom.explorer.shared.ExplorerCopyAction;
import stroom.explorer.shared.ExplorerCreateAction;
import stroom.explorer.shared.ExplorerDeleteAction;
import stroom.explorer.shared.ExplorerMoveAction;
import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.ExplorerPermissions;
import stroom.explorer.shared.FetchExplorerPermissionsAction;
import stroom.menubar.client.event.BeforeRevealMenubarEvent;
import stroom.security.client.ClientSecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.util.client.ImageUtil;
import stroom.util.shared.HasDisplayValue;
import stroom.util.shared.SharedMap;
import stroom.util.shared.VoidResult;
import stroom.widget.button.client.GlyphIcons;
import stroom.widget.menu.client.presenter.IconMenuItem;
import stroom.widget.menu.client.presenter.Item;
import stroom.widget.menu.client.presenter.MenuItem;
import stroom.widget.menu.client.presenter.MenuListPresenter;
import stroom.widget.menu.client.presenter.Separator;
import stroom.widget.menu.client.presenter.SimpleParentMenuItem;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupView.PopupType;
import stroom.widget.tab.client.event.RequestCloseAllTabsEvent;
import stroom.widget.tab.client.event.RequestCloseTabEvent;
import stroom.widget.tab.client.presenter.ImageIcon;
import stroom.widget.tab.client.presenter.TabData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityPluginEventManager extends Plugin {
    private static final KeyTest CTRL_S = event -> event.getCtrlKey() && !event.getShiftKey() && event.getKeyCode() == 'S';
    private static final KeyTest CTRL_SHIFT_S = event -> event.getCtrlKey() && event.getShiftKey() && event.getKeyCode() == 'S';
    private static final KeyTest ALT_W = event -> event.getAltKey() && !event.getShiftKey() && event.getKeyCode() == 'W';
    private static final KeyTest ALT_SHIFT_W = event -> event.getAltKey() && event.getShiftKey() && event.getKeyCode() == 'W';

    private final ClientDispatchAsync dispatcher;
    private final DocumentTypeCache documentTypeCache;
    private final MenuListPresenter menuListPresenter;
    private final ClientSecurityContext clientSecurityContext;
    private final Map<String, EntityPlugin<?>> pluginMap = new HashMap<>();
    private final KeyboardInterceptor keyboardInterceptor;
    private TabData selectedTab;
    private MultiSelectionModel<ExplorerNode> selectionModel;

    @Inject
    public EntityPluginEventManager(final EventBus eventBus,
                                    final KeyboardInterceptor keyboardInterceptor, final ClientDispatchAsync dispatcher,
                                    final DocumentTypeCache documentTypeCache, final MenuListPresenter menuListPresenter, final ClientSecurityContext clientSecurityContext) {
        super(eventBus);
        this.keyboardInterceptor = keyboardInterceptor;
        this.dispatcher = dispatcher;
        this.documentTypeCache = documentTypeCache;
        this.menuListPresenter = menuListPresenter;
        this.clientSecurityContext = clientSecurityContext;
    }

    @Override
    protected void onBind() {
        super.onBind();

        // track the currently selected content tab.
        registerHandler(getEventBus().addHandler(ContentTabSelectionChangeEvent.getType(),
                event -> selectedTab = event.getTabData()));

        // 1. Handle entity creation events.
        registerHandler(getEventBus().addHandler(CreateEntityEvent.getType(), event -> {
            dispatcher.execute(new ExplorerCreateAction(event.getEntityType(), event.getEntityName(), event.getFolder()), new AsyncCallbackAdaptor<ExplorerNode>() {
                @Override
                public void onSuccess(final ExplorerNode explorerNode) {
                    if (explorerNode != null) {
                        HighlightExplorerItemEvent.fire(EntityPluginEventManager.this, explorerNode);
                        final EntityPlugin<?> plugin = pluginMap.get(explorerNode.getType());
                        if (plugin != null) {
                            plugin.open(explorerNode.getDocRef(), true);
                        }
                    }
                }
            });

//            final EntityPlugin<?> plugin = pluginMap.get(event.getEntityType());
//            if (plugin != null) {
//                plugin.createEntity(event.getPresenter(), event.getFolder(), event.getEntityName());
//            }
        }));

        // // 2. Handle requests to close tabs.
        // registerHandler(getEventBus().addHandler(
        // RequestCloseTabEvent.getType(), new RequestCloseTabHandler() {
        // @Override
        // public void onCloseTab(final RequestCloseTabEvent event) {
        // final TabData tabData = event.getTabData();
        // if (tabData instanceof EntityTabData) {
        // final EntityTabData entityTabData = (EntityTabData) tabData;
        // final EntityPlugin<?> plugin = pluginMap
        // .get(entityTabData.getType());
        // if (plugin != null) {
        // plugin.close(entityTabData, false);
        // }
        // }
        // }
        // }));
        //
        // // 3. Handle requests to close all tabs.
        // registerHandler(getEventBus().addHandler(
        // RequestCloseAllTabsEvent.getType(), new CloseAllTabsHandler() {
        // @Override
        // public void onCloseAllTabs(
        // final RequestCloseAllTabsEvent event) {
        // for (final EntityPlugin<?> plugin : pluginMap.values()) {
        // plugin.closeAll(event.isLogoffAfterClose());
        // }
        // }
        // }));

        // 4. Handle explorer events and open items as required.
        registerHandler(
                getEventBus().addHandler(ExplorerTreeSelectEvent.getType(), event -> {
                    // Remember the selection model.
                    selectionModel = event.getSelectionModel();

                    if (!event.getSelectionType().isRightClick() && !event.getSelectionType().isMultiSelect()) {
                        final ExplorerNode explorerNode = event.getSelectionModel().getSelected();
                        if (explorerNode != null) {
                            final EntityPlugin<?> plugin = pluginMap.get(explorerNode.getType());
                            if (plugin != null) {
                                plugin.open(explorerNode.getDocRef(), event.getSelectionType().isDoubleSelect());
                            }
                        }
                    }
                }));

        // 5. Handle save events.
        registerHandler(getEventBus().addHandler(SaveEntityEvent.getType(), event -> {
            if (isDirty(event.getTabData())) {
                final EntityTabData entityTabData = event.getTabData();
                final EntityPlugin<?> plugin = pluginMap.get(entityTabData.getType());
                if (plugin != null) {
                    plugin.save(entityTabData);
                }
            }
        }));

//        // 6. Handle save as events.
//        registerHandler(getEventBus().addHandler(SaveAsEntityEvent.getType(), event -> {
//            final EntityPlugin<?> plugin = pluginMap.get(event.getTabData().getType());
//            if (plugin != null) {
//                plugin.sa(event.getDialog(), event.getTabData(), event.getEntityName());
//            }
//        }));

        // 7. Save all entities - handled directly.

        // 8.1. Handle entity copy events.
        registerHandler(getEventBus().addHandler(CopyEntityEvent.getType(), event -> {
            dispatcher.execute(new ExplorerCopyAction(event.getDocument(), event.getDocument(), event.getName()), new AsyncCallbackAdaptor<ExplorerNode>() {
                @Override
                public void onSuccess(final ExplorerNode result) {
                    // Hide the save as presenter.
                    HidePopupEvent.fire(EntityPluginEventManager.this, event.getPresenter());

                    // Create an entity item so we can open it in the editor and
                    // select it in the explorer tree.
                    HighlightExplorerItemEvent.fire(EntityPluginEventManager.this, result);
//                    final DocRef docRef = DocRef.create(entity);
//                    highlight(docRef);

//                    // The entity we had open before is now effectively closed
//                    // and the new one open so record this fact so that we can
//                    // open the old one again and the new one won't open twice.
//                    entityToTabDataMap.remove(oldEntityReference);
//                    entityToTabDataMap.put(docRef, tabData);
//                    tabDataToEntityMap.remove(tabData);
//                    tabDataToEntityMap.put(tabData, docRef);
//
//                    // Update the item in the content pane.
//                    presenter.read(entity);
                }
            });

            //            final EntityEditPresenter<?, E> presenter = (EntityEditPresenter<?, E>) tabData;
//            final E entity = presenter.getEntity();
//            presenter.write(presenter.getEntity());
//
//            final DocRef oldEntityReference = DocRef.create(entity);
//
//            DocRef folder = null;
//            if (entity instanceof HasFolder) {
//                folder = DocRef.create(((HasFolder) entity).getFolder());
//            }
//
//            copy(entity, folder, name, new SaveCallback<E>() {
//                @Override
//                public void onSave(final E entity) {
//                    // Hide the save as presenter.
//                    HidePopupEvent.fire(EntityPlugin.this, dialog);
//
//                    // Create an entity item so we can open it in the editor and
//                    // select it in the explorer tree.
//                    final DocRef docRef = DocRef.create(entity);
////                    highlight(docRef);
//
//                    // The entity we had open before is now effectively closed
//                    // and the new one open so record this fact so that we can
//                    // open the old one again and the new one won't open twice.
//                    entityToTabDataMap.remove(oldEntityReference);
//                    entityToTabDataMap.put(docRef, tabData);
//                    tabDataToEntityMap.remove(tabData);
//                    tabDataToEntityMap.put(tabData, docRef);
//
//                    // Update the item in the content pane.
//                    presenter.read(entity);
//                }
//            });


//            final EntityPlugin<?> plugin = pluginMap.get(event.getDocument().getType());
//            if (plugin != null) {
//                plugin.copyEntity(event.getPresenter(), event.getDocument(), event.getFolder(),
//                        event.getName());
//            }
        }));

        // 8.2. Handle entity move events.
        registerHandler(getEventBus().addHandler(MoveEntityEvent.getType(), event -> {


//            DocRef folder = null;
//            if (event.getFolder() != null) {
//                folder = event.getFolder().getDocRef();
//            }

            final List<ExplorerNode> children = event.getChildren();
            for (final ExplorerNode child : children) {
                dispatcher.execute(new ExplorerMoveAction(child, event.getFolder(), child.getName()), new AsyncCallbackAdaptor<ExplorerNode>() {
                    @Override
                    public void onSuccess(final ExplorerNode result) {
                        // Hide the save as presenter.
                        HidePopupEvent.fire(EntityPluginEventManager.this, event.getPresenter());

                        // Create an entity item so we can open it in the editor and
                        // select it in the explorer tree.
                        HighlightExplorerItemEvent.fire(EntityPluginEventManager.this, result);
//                    final DocRef docRef = DocRef.create(entity);
//                    highlight(docRef);

//                    // The entity we had open before is now effectively closed
//                    // and the new one open so record this fact so that we can
//                    // open the old one again and the new one won't open twice.
//                    entityToTabDataMap.remove(oldEntityReference);
//                    entityToTabDataMap.put(docRef, tabData);
//                    tabDataToEntityMap.remove(tabData);
//                    tabDataToEntityMap.put(tabData, docRef);
//
//                    // Update the item in the content pane.
//                    presenter.read(entity);
                    }
                });









//                final EntityPlugin<?> plugin = pluginMap.get(child.getType());
//                if (plugin != null) {
//                    plugin.moveEntity(event.getPresenter(), child.getDocRef(), folder);
//                }
            }
        }));

        // 9. Handle entity rename events.
        registerHandler(getEventBus().addHandler(RenameEntityEvent.getType(), event -> {
            final EntityPlugin<?> plugin = pluginMap.get(event.getDocument().getType());
            if (plugin != null) {
                plugin.renameEntity(event.getDialog(), event.getDocument(), event.getEntityName());
            }
        }));

        // 10. Handle entity delete events.
        registerHandler(getEventBus().addHandler(ExplorerTreeDeleteEvent.getType(), event -> {
            if (selectionModel != null && selectionModel.getSelectedItems().size() > 0) {
                fetchPermissions(selectionModel.getSelectedItems(), new AsyncCallbackAdaptor<SharedMap<ExplorerNode, ExplorerPermissions>>() {
                    @Override
                    public void onSuccess(final SharedMap<ExplorerNode, ExplorerPermissions> documentPermissionMap) {
                        documentTypeCache.fetch(new AsyncCallbackAdaptor<DocumentTypes>() {
                            @Override
                            public void onSuccess(final DocumentTypes documentTypes) {
                                final List<ExplorerNode> deletableItems = getExplorerDataListWithPermission(documentPermissionMap, DocumentPermissionNames.DELETE);
                                if (deletableItems.size() > 0) {
                                    deleteItems(deletableItems);
                                }
                            }
                        });
                    }
                });
            }
        }));

        // 11. Handle entity reload events.
        registerHandler(getEventBus().addHandler(ReloadEntityEvent.getType(), event -> {
            final EntityPlugin<?> plugin = pluginMap.get(event.getEntity().getType());
            if (plugin != null) {
                plugin.reload(DocRef.create(event.getEntity()));
            }
        }));

        // Not handled as it is done directly.

        registerHandler(getEventBus().addHandler(ShowNewMenuEvent.getType(), event -> {
            if (selectionModel != null && selectionModel.getSelectedItems().size() == 1) {
                final ExplorerNode primarySelection = selectionModel.getSelected();
                getNewMenuItems(primarySelection, new AsyncCallbackAdaptor<List<Item>>() {
                    @Override
                    public void onSuccess(final List<Item> children) {
                        menuListPresenter.setData(children);

                        final PopupPosition popupPosition = new PopupPosition(event.getX(), event.getY());
                        ShowPopupEvent.fire(EntityPluginEventManager.this, menuListPresenter, PopupType.POPUP,
                                popupPosition, null, event.getElement());
                    }
                });
            }
        }));
        registerHandler(getEventBus().addHandler(ShowExplorerMenuEvent.getType(), event -> {
            if (selectionModel != null) {
                final boolean singleSelection = selectionModel.getSelectedItems().size() == 1;
                final ExplorerNode primarySelection = selectionModel.getSelected();

                if (selectionModel.getSelectedItems().size() > 0) {
                    fetchPermissions(selectionModel.getSelectedItems(), new AsyncCallbackAdaptor<SharedMap<ExplorerNode, ExplorerPermissions>>() {
                        @Override
                        public void onSuccess(final SharedMap<ExplorerNode, ExplorerPermissions> documentPermissionMap) {
                            documentTypeCache.fetch(new AsyncCallbackAdaptor<DocumentTypes>() {
                                @Override
                                public void onSuccess(final DocumentTypes documentTypes) {
                                    final List<Item> menuItems = new ArrayList<>();

                                    // Only allow the new menu to appear if we have a single selection.
                                    addNewMenuItem(menuItems, singleSelection, documentPermissionMap, primarySelection, documentTypes);
                                    addModifyMenuItems(menuItems, singleSelection, documentPermissionMap);

                                    menuListPresenter.setData(menuItems);
                                    final PopupPosition popupPosition = new PopupPosition(event.getX(), event.getY());
                                    ShowPopupEvent.fire(EntityPluginEventManager.this, menuListPresenter, PopupType.POPUP,
                                            popupPosition, null);
                                }
                            });
                        }
                    });
                }
            }
        }));
    }

    private List<ExplorerNode> getExplorerDataListWithPermission(final SharedMap<ExplorerNode, ExplorerPermissions> documentPermissionMap, final String permission) {
        final List<ExplorerNode> list = new ArrayList<>();
        for (final Map.Entry<ExplorerNode, ExplorerPermissions> entry : documentPermissionMap.entrySet()) {
            if (entry.getValue().hasDocumentPermission(permission)) {
                list.add(entry.getKey());
            }
        }

        list.sort(Comparator.comparing(HasDisplayValue::getDisplayValue));
        return list;
    }

    @Override
    public void onReveal(final BeforeRevealMenubarEvent event) {
        super.onReveal(event);

        // Add menu bar item menu.
        event.getMenuItems().addMenuItem(MenuKeys.MAIN_MENU, new SimpleParentMenuItem(1, "Item", null) {
            @Override
            public void getChildren(final AsyncCallback<List<Item>> callback) {
                if (selectionModel != null) {
                    final boolean singleSelection = selectionModel.getSelectedItems().size() == 1;
                    final ExplorerNode primarySelection = selectionModel.getSelected();

                    if (selectionModel.getSelectedItems().size() > 0) {
                        fetchPermissions(selectionModel.getSelectedItems(), new AsyncCallbackAdaptor<SharedMap<ExplorerNode, ExplorerPermissions>>() {
                            @Override
                            public void onSuccess(final SharedMap<ExplorerNode, ExplorerPermissions> documentPermissionMap) {
                                documentTypeCache.fetch(new AsyncCallbackAdaptor<DocumentTypes>() {
                                    @Override
                                    public void onSuccess(final DocumentTypes documentTypes) {
                                        final List<Item> menuItems = new ArrayList<>();

                                        // Only allow the new menu to appear if we have a single selection.
                                        addNewMenuItem(menuItems, singleSelection, documentPermissionMap, primarySelection, documentTypes);
                                        menuItems.add(createCloseMenuItem(isTabItemSelected(selectedTab)));
                                        menuItems.add(createCloseAllMenuItem(isTabItemSelected(selectedTab)));
                                        menuItems.add(new Separator(5));
                                        menuItems.add(createSaveMenuItem(6, isDirty(selectedTab)));
                                        menuItems.add(createSaveAsMenuItem(7, isEntityTabData(selectedTab)));
                                        menuItems.add(createSaveAllMenuItem(8, isTabItemSelected(selectedTab)));
                                        menuItems.add(new Separator(9));
                                        addModifyMenuItems(menuItems, singleSelection, documentPermissionMap);

                                        callback.onSuccess(menuItems);
                                    }
                                });
                            }
                        });
                    }
                }
            }
        });
    }

    private void getNewMenuItems(final ExplorerNode explorerNode, final AsyncCallback<List<Item>> callback) {
        fetchPermissions(Collections.singletonList(explorerNode), new AsyncCallbackAdaptor<SharedMap<ExplorerNode, ExplorerPermissions>>() {
            @Override
            public void onSuccess(final SharedMap<ExplorerNode, ExplorerPermissions> documentPermissions) {
                documentTypeCache.fetch(new AsyncCallbackAdaptor<DocumentTypes>() {
                    @Override
                    public void onSuccess(final DocumentTypes documentTypes) {
                        callback.onSuccess(createNewMenuItems(explorerNode, documentPermissions.get(explorerNode), documentTypes));
                    }
                });
            }
        });
    }

    private void fetchPermissions(final List<ExplorerNode> explorerNodeList,
                                  final AsyncCallbackAdaptor<SharedMap<ExplorerNode, ExplorerPermissions>> callback) {
        final FetchExplorerPermissionsAction action = new FetchExplorerPermissionsAction(explorerNodeList);
        dispatcher.execute(action, callback);
    }

    private void addNewMenuItem(final List<Item> menuItems, final boolean singleSelection, final SharedMap<ExplorerNode, ExplorerPermissions> documentPermissionMap, final ExplorerNode primarySelection, final DocumentTypes documentTypes) {
        // Only allow the new menu to appear if we have a single selection.
        if (singleSelection) {
            // Add 'New' menu item.
            final ExplorerPermissions documentPermissions = documentPermissionMap.get(primarySelection);
            final List<Item> children = createNewMenuItems(primarySelection, documentPermissions,
                    documentTypes);
            final boolean allowNew = children != null && children.size() > 0;

            if (allowNew) {
                final Item newItem = new SimpleParentMenuItem(1, GlyphIcons.NEW_ITEM, GlyphIcons.NEW_ITEM, "New",
                        null, children != null && children.size() > 0, null) {
                    @Override
                    public void getChildren(final AsyncCallback<List<Item>> callback) {
                        callback.onSuccess(children);
                    }
                };
                menuItems.add(newItem);
                menuItems.add(new Separator(2));
            }
        }
    }

    private List<Item> createNewMenuItems(final ExplorerNode explorerNode,
                                          final ExplorerPermissions documentPermissions, final DocumentTypes documentTypes) {
        final List<Item> children = new ArrayList<>();

        for (final DocumentType documentType : documentTypes.getAllTypes()) {
            if (documentPermissions.hasCreatePermission(documentType)) {
                final Item item = new IconMenuItem(documentType.getPriority(), ImageIcon.create(ImageUtil.getImageURL() + documentType.getIconUrl()), null,
                        documentType.getDisplayType(), null, true, () -> ShowCreateEntityDialogEvent.fire(EntityPluginEventManager.this,
                        explorerNode, documentType.getType(), documentType.getDisplayType(), true));
                children.add(item);

                if (Folder.ENTITY_TYPE.equals(documentType.getType())) {
                    children.add(new Separator(documentType.getPriority()));
                }
            }
        }

        return children;
    }

    private void addModifyMenuItems(final List<Item> menuItems, final boolean singleSelection, final SharedMap<ExplorerNode, ExplorerPermissions> documentPermissionMap) {
        final List<ExplorerNode> readableItems = getExplorerDataListWithPermission(documentPermissionMap, DocumentPermissionNames.READ);
        final List<ExplorerNode> updatableItems = getExplorerDataListWithPermission(documentPermissionMap, DocumentPermissionNames.UPDATE);
        final List<ExplorerNode> deletableItems = getExplorerDataListWithPermission(documentPermissionMap, DocumentPermissionNames.DELETE);

        final boolean allowRead = readableItems.size() > 0;
        final boolean allowUpdate = updatableItems.size() > 0;
        final boolean allowDelete = deletableItems.size() > 0;

        menuItems.add(createCopyMenuItem(readableItems, 3, allowRead));
        menuItems.add(createMoveMenuItem(updatableItems, 4, allowUpdate));
        menuItems.add(createRenameMenuItem(updatableItems, 5, allowUpdate));
        menuItems.add(createDeleteMenuItem(deletableItems, 6, allowDelete));

        // Only allow users to change permissions if they have a single item selected.
        if (singleSelection) {
            final List<ExplorerNode> ownedItems = getExplorerDataListWithPermission(documentPermissionMap, DocumentPermissionNames.OWNER);
            if (ownedItems.size() == 1) {
                menuItems.add(new Separator(7));
                menuItems.add(createPermissionsMenuItem(ownedItems.get(0), 8, true));
            }
        }
    }

    private MenuItem createCloseMenuItem(final boolean enabled) {
        final Command command = () -> {
            if (isTabItemSelected(selectedTab)) {
                RequestCloseTabEvent.fire(EntityPluginEventManager.this, selectedTab);
            }
        };

        keyboardInterceptor.addKeyTest(ALT_W, command);

        return new IconMenuItem(3, GlyphIcons.CLOSE, GlyphIcons.CLOSE, "Close", "Alt+W", enabled,
                command);
    }

    private MenuItem createCloseAllMenuItem(final boolean enabled) {
        final Command command = () -> {
            if (isTabItemSelected(selectedTab)) {
                RequestCloseAllTabsEvent.fire(EntityPluginEventManager.this);
            }
        };

        keyboardInterceptor.addKeyTest(ALT_SHIFT_W, command);

        return new IconMenuItem(4, GlyphIcons.CLOSE, GlyphIcons.CLOSE, "Close All",
                "Alt+Shift+W", enabled, command);
    }

    private MenuItem createSaveMenuItem(final int priority, final boolean enabled) {
        final Command command = () -> {
            if (isDirty(selectedTab)) {
                final EntityTabData entityTabData = (EntityTabData) selectedTab;
                SaveEntityEvent.fire(EntityPluginEventManager.this, entityTabData);
            }
        };

        keyboardInterceptor.addKeyTest(CTRL_S, command);

        return new IconMenuItem(priority, GlyphIcons.SAVE, GlyphIcons.SAVE, "Save", "Ctrl+S",
                enabled, command);
    }

    private MenuItem createSaveAsMenuItem(final int priority, final boolean enabled) {
        final Command command = () -> {
            if (isEntityTabData(selectedTab)) {
                final EntityTabData entityTabData = (EntityTabData) selectedTab;
                ShowSaveAsEntityDialogEvent.fire(EntityPluginEventManager.this, entityTabData);
            }
        };

        return new IconMenuItem(priority, GlyphIcons.SAVE_AS, GlyphIcons.SAVE_AS, "Save As", null,
                enabled, command);
    }

    private MenuItem createSaveAllMenuItem(final int priority, final boolean enabled) {
        final Command command = () -> {
            if (isTabItemSelected(selectedTab)) {
                for (final EntityPlugin<?> plugin : pluginMap.values()) {
                    plugin.saveAll();
                }
            }
        };

        keyboardInterceptor.addKeyTest(CTRL_SHIFT_S, command);

        return new IconMenuItem(priority, GlyphIcons.SAVE, GlyphIcons.SAVE, "Save All",
                "Ctrl+Shift+S", enabled, command);
    }

    private MenuItem createCopyMenuItem(final List<ExplorerNode> explorerNodeList, final int priority, final boolean enabled) {
        final Command command = () -> {
            ShowCopyEntityDialogEvent.fire(EntityPluginEventManager.this, explorerNodeList);
        };

        return new IconMenuItem(priority, GlyphIcons.COPY, GlyphIcons.COPY, "Copy", null, enabled,
                command);
    }

    private MenuItem createMoveMenuItem(final List<ExplorerNode> explorerNodeList, final int priority, final boolean enabled) {
        final Command command = () -> ShowMoveEntityDialogEvent.fire(EntityPluginEventManager.this, explorerNodeList);

        return new IconMenuItem(priority, GlyphIcons.MOVE, GlyphIcons.MOVE, "Move", null, enabled,
                command);
    }

    private MenuItem createRenameMenuItem(final List<ExplorerNode> explorerNodeList, final int priority, final boolean enabled) {
        final Command command = () -> ShowRenameEntityDialogEvent.fire(EntityPluginEventManager.this, explorerNodeList);

        return new IconMenuItem(priority, GlyphIcons.EDIT, GlyphIcons.EDIT, "Rename", null,
                enabled, command);
    }

    private MenuItem createDeleteMenuItem(final List<ExplorerNode> explorerNodeList, final int priority, final boolean enabled) {
        final Command command = () -> deleteItems(explorerNodeList);

        return new IconMenuItem(priority, GlyphIcons.DELETE, GlyphIcons.DELETE, "Delete", null,
                enabled, command);
    }

    private MenuItem createPermissionsMenuItem(final ExplorerNode explorerNode, final int priority, final boolean enabled) {
        final Command command = () -> {
            if (explorerNode != null) {
                ShowPermissionsEntityDialogEvent.fire(EntityPluginEventManager.this, explorerNode);
            }
        };

        return new IconMenuItem(priority, GlyphIcons.PERMISSIONS, GlyphIcons.PERMISSIONS, "Permissions", null,
                enabled, command);
    }

    private void deleteItems(final List<ExplorerNode> explorerNodeList) {
        if (explorerNodeList != null) {
            for (final ExplorerNode explorerNode : explorerNodeList) {




                        ConfirmEvent.fire(EntityPluginEventManager.this, "Are you sure you want to delete " + explorerNode.getType() + " '"
                                + explorerNode.getDisplayValue() + "'?", new ConfirmCallback() {
                            @Override
                            public void onResult(final boolean result) {
                                dispatcher.execute(new ExplorerDeleteAction(explorerNode), new AsyncCallbackAdaptor<VoidResult>() {
                                    @Override
                                    public void onSuccess(final VoidResult result) {
                                        RefreshExplorerTreeEvent.fire(EntityPluginEventManager.this);
                                    }
                                });

//                                if (result) {
//                                    // We need to load the entity here as it hasn't
//                                    // been loaded yet.
//                                    load(docRef, new EntityPlugin.LoadCallback<E>() {
//                                        @Override
//                                        public void onLoad(final E entity) {
//                                            deleteEntity(entity, null);
//                                        }
//                                    });
//                                }
                            }
                        });
//                    }
//                }
//
//                delete(entity, new EntityPlugin.DeleteCallback<E>() {
//                    @Override
//                    public void onDelete(final E entity) {
//                        if (tabData != null) {
//                            // Cleanup reference to this tab data.
//                            removeTabData(tabData);
//                            contentManager.forceClose(tabData);
//                        }
//                        // Refresh the explorer tree so the entity is marked as deleted.
//                        RefreshExplorerTreeEvent.fire(EntityPlugin.this);
//                    }
//                });
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//                    final EntityPlugin<?> plugin = pluginMap.get(explorerNode.getType());
//                    if (plugin != null) {
//                        plugin.deleteEntity(explorerNode.getDocRef());
//                    }
            }
        }
    }

    void registerPlugin(final String entityType, final EntityPlugin<?> plugin) {
        pluginMap.put(entityType, plugin);
    }

    private boolean isTabItemSelected(final TabData tabData) {
        return tabData != null;
    }

    private boolean isEntityTabData(final TabData tabData) {
        if (isTabItemSelected(tabData)) {
            if (tabData instanceof EntityEditPresenter<?, ?>) {
                return true;
            }
        }

        return false;
    }

    private boolean isDirty(final TabData tabData) {
        if (isEntityTabData(tabData)) {
            final EntityEditPresenter<?, ?> editPresenter = (EntityEditPresenter<?, ?>) tabData;
            if (editPresenter.isDirty()) {
                return true;
            }
        }

        return false;
    }
}
