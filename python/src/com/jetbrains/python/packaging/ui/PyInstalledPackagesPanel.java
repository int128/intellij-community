/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python.packaging.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.webcore.packaging.InstalledPackage;
import com.intellij.webcore.packaging.InstalledPackagesPanel;
import com.intellij.webcore.packaging.PackageManagementService;
import com.intellij.webcore.packaging.PackagesNotificationPanel;
import com.jetbrains.python.packaging.*;
import com.jetbrains.python.sdk.PySdkUtil;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author yole
 */
public class PyInstalledPackagesPanel extends InstalledPackagesPanel {
  private boolean myHasManagement = false;

  public PyInstalledPackagesPanel(Project project, PackagesNotificationPanel area) {
    super(project, area);
  }

  private Sdk getSelectedSdk() {
    PyPackageManagementService service = (PyPackageManagementService)myPackageManagementService;
    return service != null ? service.getSdk() : null;
  }

  class PyInstallPackageManagementFix implements PyExecutionFix {
    @NotNull
    @Override
    public String getName() {
      return "Install packaging tools";
    }

    @Override
    public void run(@NotNull final Sdk sdk) {
      final PyPackageManagerUI ui = new PyPackageManagerUI(myProject, sdk, new PyPackageManagerUI.Listener() {
        @Override
        public void started() {
          myPackagesTable.setPaintBusy(true);
        }

        @Override
        public void finished(List<ExecutionException> exceptions) {
          myPackagesTable.setPaintBusy(false);
          PyPackageManager packageManager = PyPackageManager.getInstance(sdk);
          final PackageManagementService.ErrorDescription description = PyPackageManagementService.toErrorDescription(exceptions, sdk);
          if (description != null) {
            PackagesNotificationPanel.showError("Failed to install Python packaging tools", description);
          }
          packageManager.refresh();
          updatePackages(PyPackageManagers.getInstance().getManagementService(myProject, sdk));
          updateNotifications(sdk);
        }
      });
      ui.installManagement();
    }
  }

  public void updateNotifications(@Nullable final Sdk selectedSdk) {
    if (selectedSdk == null) {
      myNotificationArea.hide();
      return;
    }
    final Application application = ApplicationManager.getApplication();
    application.executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        PyExecutionException exception = null;
        try {
          myHasManagement = PyPackageManager.getInstance(selectedSdk).hasManagement(false);
          if (!myHasManagement) {
            throw new PyExecutionException("Python packaging tools not found", "pip", Collections.<String>emptyList(), "", "", 0,
                                           ImmutableList.of(new PyInstallPackageManagementFix()));
          }
        }
        catch (PyExecutionException e) {
          exception = e;
        }
        catch (ExecutionException e) {
          return;
        }
        final PyExecutionException problem = exception;
        application.invokeLater(new Runnable() {
          @Override
          public void run() {
            if (selectedSdk == getSelectedSdk()) {
              myNotificationArea.hide();
              if (problem != null) {
                final boolean invalid = PythonSdkType.isInvalid(selectedSdk);
                if (!invalid) {
                  final StringBuilder builder = new StringBuilder(problem.getMessage());
                  builder.append(". ");
                  for (final PyExecutionFix fix : problem.getFixes()) {
                    final String key = "id" + fix.hashCode();
                    final String link = "<a href=\"" + key + "\">" + fix.getName() + "</a>";
                    builder.append(link);
                    builder.append(" ");
                    myNotificationArea.addLinkHandler(key, new Runnable() {
                      @Override
                      public void run() {
                        final Sdk sdk = getSelectedSdk();
                        if (sdk != null) {
                          fix.run(sdk);
                          myNotificationArea.removeLinkHandler(key);
                          updatePackages(PyPackageManagers.getInstance().getManagementService(myProject, sdk));
                          updateNotifications(sdk);
                        }
                      }
                    });
                  }
                  myNotificationArea.showWarning(builder.toString());
                }
                myInstallButton.setEnabled(!invalid && installEnabled());
              }
            }
          }
        }, ModalityState.any());
      }
    });
  }

  @Override
  protected Set<String> getPackagesToPostpone() {
    return Sets.newHashSet("pip", "distutils", "setuptools");
  }

  @Override
  protected boolean canUninstallPackage(InstalledPackage pkg) {
    if (!myHasManagement) return false;

    if (PythonSdkType.isDocker(getSelectedSdk())) {
      return false;
    }

    if (PythonSdkType.isVirtualEnv(getSelectedSdk()) && pkg instanceof PyPackage) {
      final String location = ((PyPackage)pkg).getLocation();
      if (location != null && location.startsWith(PySdkUtil.getUserSite())) {
        return false;
      }
    }
    final String name = pkg.getName();
    if (PyPackageManager.PIP.equals(name) ||
        PyPackageManager.SETUPTOOLS.equals(name) ||
        PyPackageManager.DISTRIBUTE.equals(name) ||
        PyCondaPackageManagerImpl.PYTHON.equals(name)) {
      return false;
    }
    return true;
  }

  @Override
  protected boolean canInstallPackage(@NotNull final InstalledPackage pyPackage) {
    return installEnabled();
  }

  @Override
  protected boolean installEnabled() {
    if (PythonSdkType.isDocker(getSelectedSdk())) {
      return false;
    }

    return myHasManagement;
  }

  @Override
  protected boolean canUpgradePackage(InstalledPackage pyPackage) {
    if (PythonSdkType.isDocker(getSelectedSdk())) {
      return false;
    }

    return myHasManagement && !PyCondaPackageManagerImpl.PYTHON.equals(pyPackage.getName());
  }
}
