/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 * This file is part of Alfresco
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.share;

import java.util.List;

import org.alfresco.po.share.AlfrescoVersion;
import org.alfresco.po.share.site.document.DocumentDetailsPage;
import org.alfresco.po.share.site.document.DocumentLibraryPage;
import org.alfresco.po.share.site.document.FolderDetailsPage;
import org.alfresco.share.util.AbstractUtils;
import org.alfresco.share.util.ShareUser;
import org.alfresco.share.util.ShareUserSitePage;
import org.alfresco.share.util.WebDroneType;
import org.alfresco.share.util.api.CreateUserAPI;
import org.alfresco.test.FailedTestListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * @author cbairaajoni
 */
@Listeners(FailedTestListener.class)
public class DownloadFilesAndFoldersTest extends AbstractUtils
{
    private static Log logger = LogFactory.getLog(DownloadFilesAndFoldersTest.class);
    private static final String FILE_ZIP_EXT = ".zip";
    private static String FILE_WITH_UNSUPPORTED_FORMAT = "Test_3893_Unsupported_Format.mm";
    private static String DOC_FILE_WITH_UNSUPPORTED_MAX_SIZE = "Test_3893_Unsupported_Max_Size.doc";

    /**
     * Pre test setup of a dummy file to upload.
     * 
     * @throws Exception
     */
    @Override
    @BeforeClass(alwaysRun = true)
    public void setup() throws Exception
    {
        super.setupCustomDrone(WebDroneType.DownLoadDrone);
        testName = this.getClass().getSimpleName();
        logger.info("[Suite ] : Start Tests in: " + testName);
    }

    /**
     * This test data setup is combination of AONE-10596/5655/5656
     * DataPreparation method - AONE-10596
     * <ul>
     * <li>Login</li>
     * <li>Create User</li>
     * <li>Create Site</li>
     * <li>Create folders with testname, special characters and native characters.</li>
     * </ul>
     * 
     * @throws Exception
     */
    @Test(groups = { "DataPrepDownload", "EnterpriseOnly" })
    public void dataPrep_AdvSearch_10596() throws Exception
    {
        String testName = getTestName();
        String siteName = getSiteName(testName);
        String testUser = getUserNameFreeDomain(testName);
        String[] testUserInfo = new String[] { testUser };
        String folderName = getFolderName(testName);
        String folderName_with_splChars = "!@";
        String folderName_with_nativeChars = "désir Bedürfnis";

        try
        {
            // User
            CreateUserAPI.CreateActivateUser(customDrone, ADMIN_USERNAME, testUserInfo);

            // Login
            ShareUser.login(customDrone, testUser, DEFAULT_PASSWORD);

            // Site
            ShareUser.createSite(customDrone, siteName, AbstractUtils.SITE_VISIBILITY_PUBLIC).render(maxWaitTime);

            // Creating folder.
            ShareUserSitePage.createFolder(customDrone, folderName, null);
            ShareUserSitePage.createFolder(customDrone, folderName_with_splChars, null);
            ShareUserSitePage.createFolder(customDrone, folderName_with_nativeChars, null);
        }
        catch (Throwable e)
        {
            reportError(customDrone, testName, e);
        }
        finally
        {
            testCleanup(customDrone, testName);
        }
    }

    /**
     * This test is combination of AONE-10596/10597/10597
     * Test - AONE-10596: Empty folder. Download as ZIP
     * <ul>
     * <li>Login</li>
     * <li>From My Site Document Library access the folder view details page</li>
     * <li>Select Download as zip</li>
     * <li>Extract folder</li>
     * <li>Verify the results as expected</li>
     * </ul>
     */
    @Test(groups = { "NonGrid", "EnterpriseOnly", "Download" })
    public void AONE_10596()
    {
        /** Start Test */
        testName = getTestName();
        String testUser = getUserNameFreeDomain(testName);
        String siteName = getSiteName(testName);
        String folderName = getFolderName(testName);
        String folderName_with_splChars = "!@";
        String folderName_with_nativeChars = "désir Bedürfnis";

        if (alfrescoVersion.equals(AlfrescoVersion.Enterprise41) || isAlfrescoVersionCloud(customDrone))
        {
            throw new UnsupportedOperationException("Download as zip functionality is not available in Product Version");
        }

        // Login
        ShareUser.login(customDrone, testUser, DEFAULT_PASSWORD);

        // Download and verifying the normal folder

        // Opening site document library
        DocumentLibraryPage docLibPage = ShareUser.openSitesDocumentLibrary(customDrone, siteName);

        // Open Folder Details Page
        FolderDetailsPage folderDetailsPage = ShareUserSitePage.getContentDetailsPage(customDrone, folderName).render();

        // Select DownloadFolder as Zip
        folderDetailsPage = folderDetailsPage.selectDownloadFolderAsZip("folder").render();
        folderDetailsPage.waitForFile(downloadDirectory + folderName + FILE_ZIP_EXT);

        // Thread needs to be stopped for some time to make sure the file download stream has been closed properly
        // otherwise download part will not be closed and will throws an exception.
        // TODO: Chiran: If its necessary to wait, incorporate it in waitForFile method
        webDriverWait(customDrone, 3000);
        
        Assert.assertTrue(ShareUser.extractDownloadedArchieve(customDrone, folderName + FILE_ZIP_EXT));

        // Download and verifying the folder with special characters
        docLibPage = ShareUser.openDocumentLibrary(customDrone);
        folderDetailsPage = docLibPage.getFileDirectoryInfo(folderName_with_splChars).selectViewFolderDetails().render();
        folderDetailsPage = folderDetailsPage.selectDownloadFolderAsZip("folder").render();
        folderDetailsPage.waitForFile(downloadDirectory + folderName_with_splChars + FILE_ZIP_EXT);

        // Thread needs to be stopped for some time to make sure the file download stream has been closed properly
        // otherwise download part will not be closed and will throws an exception.
        webDriverWait(customDrone, 3000);

        Assert.assertTrue(ShareUser.extractDownloadedArchieve(customDrone, folderName_with_splChars + FILE_ZIP_EXT));

        // Download and verifying the folder with native characters
        docLibPage = ShareUser.openDocumentLibrary(customDrone);
        folderDetailsPage = docLibPage.getFileDirectoryInfo(folderName_with_nativeChars).selectViewFolderDetails().render();
        folderDetailsPage = folderDetailsPage.selectDownloadFolderAsZip("folder").render();
        folderDetailsPage.waitForFile(downloadDirectory + folderName_with_nativeChars + FILE_ZIP_EXT);

        // Thread needs to be stopped for some time to make sure the file download stream has been closed properly
        // otherwise download part will not be closed and will throws an exception.
        webDriverWait(customDrone, 3000);

        Assert.assertTrue(ShareUser.extractDownloadedArchieve(customDrone, folderName_with_nativeChars + FILE_ZIP_EXT));
    }

    /**
     * DataPreparation method - AONE-10597
     * <ul>
     * <li>Login</li>
     * <li>Create User</li>
     * <li>Create Site</li>
     * <li>Create folder</li>
     * <li>Any files created inside the folder</li>
     * <li>Any folder is created inside the folder</li>
     * </ul>
     * 
     * @throws Exception
     */
    @Test(groups = { "DataPrepDownload", "EnterpriseOnly" })
    public void dataPrep_AdvSearch_10597() throws Exception
    {
        String testName = getTestName();
        String siteName = getSiteName(testName);
        String testUser = getUserNameFreeDomain(testName);
        String[] testUserInfo = new String[] { testUser };
        String folderName1 = getFolderName(testName) + "test1";
        String folderName2 = getFolderName(testName) + "test2";

        try
        {
            // User
            CreateUserAPI.CreateActivateUser(customDrone, ADMIN_USERNAME, testUserInfo);

            // Login
            ShareUser.login(customDrone, testUser, DEFAULT_PASSWORD);

            // Site
            ShareUser.createSite(customDrone, siteName, AbstractUtils.SITE_VISIBILITY_PUBLIC).render(maxWaitTime);

            // Creating folder.
            ShareUserSitePage.createFolder(customDrone, folderName1, null);

            // Uploading three files.
            String fileInfo[] = { getFileName(siteName + "_file1"), folderName1 };
            ShareUser.uploadFileInFolder(customDrone, fileInfo);

            fileInfo[0] = getFileName(siteName + "_file2");
            fileInfo[1] = folderName1;
            ShareUser.uploadFileInFolder(customDrone, fileInfo);

            fileInfo[0] = getFileName(siteName + "_file3");
            ShareUser.uploadFileInFolder(customDrone, fileInfo);

            // Creating folder inside the main folder.
            ShareUser.createFolderInFolder(customDrone, folderName2, "", folderName1);

        }
        catch (Throwable e)
        {
            reportError(customDrone, testName, e);
        }
        finally
        {
            testCleanup(customDrone, testName);
        }
    }

    /**
     * Test - AONE-10597: Non Empty folder. Download as ZIP
     * <ul>
     * <li>Login</li>
     * <li>From My Site Document Library access the folder view details page</li>
     * <li>Select Download as zip</li>
     * <li>Extract folder</li>
     * <li>Verify the results as expected</li>
     * </ul>
     */
    @Test(groups = { "NonGrid", "EnterpriseOnly", "Download" })
    public void AONE_10597()
    {
        /** Start Test */
        testName = getTestName();
        String testUser = getUserNameFreeDomain(testName);
        String siteName = getSiteName(testName);
        String folderName1 = getFolderName(testName) + "test1";
        String folderName2 = getFolderName(testName) + "test2";

        if (alfrescoVersion.equals(AlfrescoVersion.Enterprise41) || isAlfrescoVersionCloud(customDrone))
        {
            throw new UnsupportedOperationException("Download as zip functionality is not available in Product Version");
        }

        // Login
        ShareUser.login(customDrone, testUser, DEFAULT_PASSWORD);

        // Opening site document library
        DocumentLibraryPage docLibPage = ShareUser.openSitesDocumentLibrary(customDrone, siteName);

        // Open Folder Details Page
        FolderDetailsPage folderDetailsPage = docLibPage.getFileDirectoryInfo(folderName1).selectViewFolderDetails().render();

        // Select DownloadFolder as Zip
        folderDetailsPage.selectDownloadFolderAsZip("folder");
        folderDetailsPage.waitForFile(downloadDirectory + folderName1 + FILE_ZIP_EXT);

        // Thread needs to be stopped for some time to make sure the file download stream has been closed properly
        // otherwise download part will not be closed and will throws an exception.
        webDriverWait(customDrone, 3000);

        // Extract the zip folder.
        Assert.assertTrue(ShareUser.extractDownloadedArchieve(customDrone, folderName1 + FILE_ZIP_EXT));

        folderDetailsPage.waitForFile(downloadDirectory + folderName1);

        List<String> extractedChildFilesOrFolders = ShareUser.getContentsOfDownloadedArchieve(customDrone, (downloadDirectory + folderName1));

        Assert.assertNotNull(extractedChildFilesOrFolders);
        Assert.assertTrue(extractedChildFilesOrFolders.size() == 4);
        Assert.assertTrue(extractedChildFilesOrFolders.contains(getFileName(siteName + "_file1")));
        Assert.assertTrue(extractedChildFilesOrFolders.contains(getFileName(siteName + "_file2")));
        Assert.assertTrue(extractedChildFilesOrFolders.contains(getFileName(siteName + "_file3")));
        Assert.assertTrue(extractedChildFilesOrFolders.contains(folderName2));
    }

    @Test(groups = { "DataPrepDownload" })
    public void dataPrep_AONE_2008() throws Exception
    {
        String testName = getTestName();
        String testUser = getUserNameFreeDomain(testName);
        String siteName = getSiteName(testName);
        String fileName = FILE_WITH_UNSUPPORTED_FORMAT;

        try
        {
            // User
            String[] testUserInfo = new String[] { testUser };
            CreateUserAPI.CreateActivateUser(customDrone, ADMIN_USERNAME, testUserInfo);

            // Login
            ShareUser.login(customDrone, testUser, DEFAULT_PASSWORD);

            // Site
            ShareUser.createSite(customDrone, siteName, AbstractUtils.SITE_VISIBILITY_PUBLIC).render(maxWaitTime);

            ShareUser.openSitesDocumentLibrary(customDrone, siteName).render(maxWaitTime);

            // Uploading unsupported format (*.mm)
            String[] fileInfo = { fileName };
            ShareUser.uploadFileInFolder(customDrone, fileInfo);

            // Uploading file with unsupported max size. (*.doc) and default max size for doc file is < 10MB
            fileInfo[0] = DOC_FILE_WITH_UNSUPPORTED_MAX_SIZE;
            ShareUser.uploadFileInFolder(customDrone, fileInfo);
        }
        catch (Throwable e)
        {
            reportError(customDrone, testName, e);
        }
        finally
        {
            testCleanup(customDrone, testName);
        }
    }

    /**
     * This test is valid for Cloud and Enterprise4.1 only. Enterprise4.2 UI development changes are in progress. Test:
     * <ul>
     * <li>Login</li>
     * <li>Create Site</li>
     * <li>Open Document library page</li>
     * <li>select added any unsupported document</li>
     * <li>Document details page opens</li>
     * <li>
     * <li>verify document preview should not be displayed</li>
     * <li>verify download link should displayed</li>
     * <li>click on download link</li>
     * </ul>
     */
    @Test(groups = { "NonGrid", "Download" })
    public void AONE_2008()
    {
        try
        {
            /** Start Test */
            String testName = getTestName();
            String testUser = getUserNameFreeDomain(testName);
            String siteName = getSiteName(testName);

            // Login
            ShareUser.login(customDrone, testUser, DEFAULT_PASSWORD);

            ShareUser.openSiteDashboard(customDrone, siteName);

            // Open document library page.
            DocumentLibraryPage docLibPage = ShareUser.openDocumentLibrary(customDrone);

            // Select unsupported formatted file
            DocumentDetailsPage documentDetailsPage = docLibPage.selectFile(FILE_WITH_UNSUPPORTED_FORMAT).render();

            Boolean isNoPreviewMessage = documentDetailsPage.isNoPreviewMessageDisplayed();

            Assert.assertTrue(isNoPreviewMessage);

            // Previewing and Download links for unsupported max size file is available for Enterprise 4.1 and 4.2 only.
            // Download links is available for Enterprise 4.1 and 4.2 only.
            if (!alfrescoVersion.isCloud())
            {
                documentDetailsPage.clickOnDownloadLinkForUnsupportedDocument();
                documentDetailsPage.waitForFile(downloadDirectory + FILE_WITH_UNSUPPORTED_FORMAT);

                Assert.assertTrue(ShareUser.getContentsOfDownloadedArchieve(customDrone, downloadDirectory).contains(FILE_WITH_UNSUPPORTED_FORMAT));

                // Previewing and Download links for unsupported max size file is available for Enterprise 4.1 and 4.2 only.
                // Open document library page.
                docLibPage = ShareUser.openDocumentLibrary(customDrone).render();

                // Select unsupported max size file
                documentDetailsPage = docLibPage.selectFile(DOC_FILE_WITH_UNSUPPORTED_MAX_SIZE).render();
                isNoPreviewMessage = documentDetailsPage.isNoPreviewMessageDisplayed();

                Assert.assertTrue(isNoPreviewMessage, "Unable to show download link for unsupported max size file");

                documentDetailsPage.clickOnDownloadLinkForUnsupportedDocument();
                // TODO: Chiran: Create a download file method / utility to click on download and wait for the file for the configurable amount of time, break
                // if file isn't downloaded in stipulated time
                documentDetailsPage.waitForFile(downloadDirectory + DOC_FILE_WITH_UNSUPPORTED_MAX_SIZE);

                Boolean result = ShareUser.getContentsOfDownloadedArchieve(customDrone, downloadDirectory).contains(DOC_FILE_WITH_UNSUPPORTED_MAX_SIZE);
                Assert.assertTrue(result);
            }
        }
        catch (Throwable e)
        {
            reportError(customDrone, testName, e);
        }
        finally
        {
            testCleanup(customDrone, testName);
        }
    }

    @Test(groups = { "DataPrepDownload" })
    public void dataPrep_testDownloadFolders_10595() throws Exception
    {
        String testName = getTestName();
        String testUser = getUserNameFreeDomain(testName);
        String siteName = getSiteName(testName);
        String folderName = getFolderName(testName);

        try
        {
            // Create user
            String[] testUserInfo = new String[] { testUser };
            CreateUserAPI.CreateActivateUser(customDrone, ADMIN_USERNAME, testUserInfo);

            // Enterprise Login
            ShareUser.login(customDrone, testUser, DEFAULT_PASSWORD);

            // Site
            ShareUser.createSite(customDrone, siteName, AbstractUtils.SITE_VISIBILITY_PUBLIC).render();

            ShareUser.openSitesDocumentLibrary(customDrone, siteName).render();

            // Create an empty folder.
            ShareUserSitePage.createFolder(customDrone, folderName, null);

            // Create a content
            String fileInfo[] = { getFileName(siteName) };
            ShareUser.uploadFileInFolder(customDrone, fileInfo);
        }
        catch (Throwable e)
        {
            reportError(customDrone, testName, e);
        }
        finally
        {
            testCleanup(customDrone, testName);
        }
    }

    /**
     * Note: First four steps of this test case in testlink are implemented as part of AONE-10596,AONE-10597.
     * <ul>
     * <li>Login</li>
     * <li>Create Site</li>
     * <li>Open Document library page</li>
     * <li>select added folders and document</li>
     * <li>click on download as zip</li.
     * <li>Extract archieve and verify the folders size.</li.
     * </ul>
     */
    @Test(groups = { "Download", "EnterpriseOnly", "NonGrid" })
    public void AONE_10595()
    {
        /** Start Test */
        String testName = getTestName();
        String testUser = getUserNameFreeDomain(testName);
        String siteName = getSiteName(testName);
        String folderName = getFolderName(testName);
        String fileName = getFileName(siteName);

        // Login
        ShareUser.login(customDrone, testUser, DEFAULT_PASSWORD);

        ShareUser.openSiteDashboard(customDrone, siteName);
        // Open document library page.
        DocumentLibraryPage docLibPage = ShareUser.openDocumentLibrary(customDrone);

        docLibPage.getFileDirectoryInfo(folderName).selectCheckbox();
        docLibPage.getFileDirectoryInfo(fileName).selectCheckbox();
        docLibPage = docLibPage.getNavigation().clickSelectedItems().render();
        docLibPage = docLibPage.getNavigation().clickSelectedItems().render();
        docLibPage = docLibPage.getNavigation().selectDownloadAsZip().render();

        docLibPage.waitForFile(downloadDirectory + "Archive.zip");

        // Thread needs to be stopped for some time to make sure the file download stream has been closed properly
        // otherwise download part will not be closed and will throws an exception.
        webDriverWait(customDrone, 3000);

        // Extract the zip folder.
        Assert.assertTrue(ShareUser.extractDownloadedArchieve(customDrone, "Archive.zip"));

        docLibPage.waitForFile(downloadDirectory + folderName);
        docLibPage.waitForFile(downloadDirectory + fileName);

        List<String> filesOfFolders = ShareUser.getContentsOfDownloadedArchieve(customDrone, downloadDirectory);

        Assert.assertTrue(filesOfFolders.contains(folderName));
        Assert.assertTrue(filesOfFolders.contains(fileName));
    }
}