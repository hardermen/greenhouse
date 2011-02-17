/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.springsource.greenhouse.invite;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.social.connect.ServiceProvider;
import org.springframework.social.connect.support.ConnectionRepository;
import org.springframework.social.facebook.FacebookApi;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.flash.FlashMap;

import com.springsource.greenhouse.account.Account;
import com.springsource.greenhouse.account.AccountRepository;
import com.springsource.greenhouse.account.ProfileReference;

/**
 * UI Controller for inviting Facebook friends to join our community.
 * @author Keith Donald
 */
@Controller
public class FacebookInviteController {

	private final ServiceProvider<FacebookApi> facebookProvider;
	private final ConnectionRepository connectionRepository;
	private final AccountRepository accountRepository;
	
	/**
	 * Construct the FacebookInviteController.
	 * The Facebook ServiceProvider is injected for obtaining the client-side proxy to Facebook's REST API.
	 * It is also used to lookup which of a member's Facebook friends have already joined our community.
	 */
	@Inject
	public FacebookInviteController(ServiceProvider<FacebookApi> facebookProvider, ConnectionRepository connectionRepository, AccountRepository accountRepository) {
		this.facebookProvider = facebookProvider;
		this.connectionRepository = connectionRepository;
		this.accountRepository = accountRepository;
	}
	
	/**
	 * Renders the Facebook friend-finder page as HTML in the web browser.
	 * If the member's account is connected to Facebook, adds his or her Facebook friends who are already members to the model.
	 * Supports distinguishing between those Facebook friends who have already joined and those who haven't.
	 */
	@RequestMapping(value="/invite/facebook", method=RequestMethod.GET)
	public void friendFinder(Model model, Account account) {
		if (facebookProvider.isConnected(account.getId())) {
			List<String> facebookIds = facebookProvider.getConnections(account.getId()).get(0).getServiceApi().getFriendIds();
			List<ProfileReference> profileReferences = accountRepository.findProfileReferencesByIds(accountIds(facebookIds));
			model.addAttribute("friends", profileReferences);
		} else {
			model.addAttribute("friends", Collections.emptySet());
		}
	}
	
	/**
	 * Called by the Facebook multi-friend-selector form after sending invitations.
	 * Redirects the user back to the invite page and renders a success message.
	 * @param inviteIds the invitation ids assigned by Facebook, one for each invitation sent
	 */
	@RequestMapping(value="/invite/facebook/request-form", method=RequestMethod.POST)
	public String invitationsSent(@RequestParam("ids[]") String[] inviteIds) {
		FlashMap.setSuccessMessage("Your invitations have been sent");
		return "redirect:/invite";
	}

	/**
	 * Called by the Facebook multi-friend-selector form after choosing to cancel the sending of one or more invites.
	 * Redirects the user back to the main invite page.
	 */
	@RequestMapping(value="/invite/facebook/request-form", method=RequestMethod.GET)
	public String invitationsCancelled() {
		return "redirect:/invite";
	}

	private List<Long> accountIds(List<String> screenNames) {
		List<Serializable> matches = connectionRepository.findAccountIdsForProviderAccountIds("twitter", screenNames);
		List<Long> accountIds = new ArrayList<Long>();
		for (Serializable match : matches) {
			accountIds.add(Long.valueOf(String.valueOf(match)));
		}
		return accountIds;
	}
}