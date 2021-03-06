/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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
package org.alfresco.po.alfresco;

import org.alfresco.webdrone.RenderTime;
import org.alfresco.webdrone.WebDrone;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

/**
 * Represents the repository admin console page found on alfresco.
 * @author Michael Suzuki
 * @since 5.0
 */
public class RepositoryAdminConsolePage extends AbstractAdminConsole
{

    private final static By INPUT_FIELD = By.name("repo-cmd"); 
    
    public RepositoryAdminConsolePage(WebDrone drone)
    {
        super(drone);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RepositoryAdminConsolePage render(RenderTime timer)
    {
        basicRender(timer);
        while (true)
        {
            timer.start();
            try
            {
                if (drone.find(INPUT_FIELD).isDisplayed() && drone.find(SUBMIT_BUTTON).isDisplayed())
                {
                    break;
                }
            }
            catch (NoSuchElementException nse){ }
            finally
            {
                timer.end();
            }
        }
        return this;
    }
    @SuppressWarnings("unchecked")
    @Override
    public RepositoryAdminConsolePage render(long time)
    {
        return render(new RenderTime(time));
    }
    @SuppressWarnings("unchecked")
    @Override
    public RepositoryAdminConsolePage render()
    {
        return render(maxPageLoadingTime);
    }
    /**
     * Populates the input with command and
     * submits the form.
     * @param command String command
     */
    public void sendCommands(final String command)
    {
        WebElement input = drone.find(INPUT_FIELD);
        input.clear();
        input.sendKeys(command);
        drone.find(SUBMIT_BUTTON).click();
    }
}
