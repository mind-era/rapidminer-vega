/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.util.Map;

import javax.mail.Session;

import com.rapidminer.RapidMiner;

/**
 * 
 * @author Simon Fischer
 *
 */
public class MailUtilities {

    private static MailSessionFactory mailFactory = new DefaultMailSessionFactory();

    public static void setMailSessionFactory(MailSessionFactory mailFactory) {
        MailUtilities.mailFactory = mailFactory;
    }

    public static void sendEmail(String address, String subject, String content) {
        sendEmail(address, subject, content, null);
    }

    /**
     * Sends a mail to the given address, using the specified subject and contents. Subject must contain no whitespace!
     * @param headers
     */
    public static void sendEmail(String address, String subject, String content, Map<String, String> headers) {
        try {
            String method = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD);
            int methodIndex = -1;
            if (method != null) {
                try {
                    methodIndex = Integer.parseInt(method);
                } catch (NumberFormatException e) {
                    methodIndex = -1;
                    for (int i = 0; i < RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES.length; i++) {
                        if (RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES[i].equals(method)) {
                            methodIndex = i;
                            break;
                        }
                    }
                }
            }
            if (methodIndex == -1) {
                methodIndex = RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP;
            }

            MailSender mailSender = null;
            switch (methodIndex) {
            case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP:
                mailSender = new MailSenderSMTP();
                break;
            case RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SENDMAIL:
                mailSender = new MailSenderSendmail();
                break;
            default:
                LogService.getGlobal().log("Illegal send mail method: " + method + ".", LogService.ERROR);
            }

            if (mailSender != null) {
                mailSender.sendEmail(address, subject, content, headers);
                LogService.getRoot().info("Sent mail to "+address+" with subject "+subject);
            }
        } catch (Exception e) {
            LogService.getGlobal().log("Cannot send mail to " + address + ": " + e, LogService.ERROR);
        }
    }

    public static Session makeSession() {
        return mailFactory.makeSession();
    }
}
