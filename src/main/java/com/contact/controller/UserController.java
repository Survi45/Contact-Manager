package com.contact.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.contact.entities.Contact;
import com.contact.entities.User;
import com.contact.helper.Message;
import com.contact.repository.ContactRepository;
import com.contact.repository.UserRepository;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model m, Principal principal) {
		
		String userName = principal.getName();
		//System.out.println("Username "+userName);
		//get the user using username(Email)
		User user = this.userRepository.getUserByUserName(userName);
		//System.out.println("user "+user);
		
		m.addAttribute("user", user);
				
	}
	
	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		
		model.addAttribute("title", "User dashboard");	
		return "normal/user_dashboard";
		
	}
	
	//open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@Valid @ModelAttribute Contact contact, BindingResult bindingResult,@RequestParam("profileImage") MultipartFile file, Principal principal, HttpSession session) {
		
		try {
				
			if(bindingResult.hasErrors()) {
				System.out.println(bindingResult.toString());
				return "normal/add_contact_form";
			}
			
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			//processing and uploading file....
			
			if(file.isEmpty()) {
				//if the file is empty then try our message
				System.out.println("File is empty");
				contact.setImage("contact.png");
			}else {
				//upload the file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());
				
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
            			
			user.getContacts().add(contact);
			contact.setUser(user);
			
			this.userRepository.save(user);
			
			System.out.println("Data "+contact);
			System.out.println("Added to database");
			
			// success message
			session.setAttribute("message", new Message("Your Contact is added !! Add more..", "alert-success"));
			
			
			
		}catch (Exception e) {
			e.printStackTrace();
			//error message
			session.setAttribute("message", new Message("Something went wrong !! Try again...", "alert-danger"));
		}
		return "normal/add_contact_form";
		
		
	}
	//show contacts handler
	//per page= 5[n]
	//current page=0 [page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {
		m.addAttribute("title", "Show User Contacts");
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		
		//current page
		//contact per page-5
		Pageable pageable = PageRequest.of(page, 1);
		
		
	    Page<Contact> contacts= this.contactRepository.findContactsByUser(user.getId(),pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		
				
		return "normal/show_contacts";	
	}
	
	//showing particular contact details
	
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("cID :"+ cId);
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		//
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
			
		}
		
		return "normal/contact_detail";
		
	}
	
	//delete contact hendler
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Principal principal, HttpSession session) {
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		
		if(user.getId()==contact.getUser().getId()) {
			
			contact.setUser(null);
			this.contactRepository.delete(contact);
		}
		session.setAttribute("message", new Message("Contact deleted successfully", "alert-success"));
		return "redirect:/user/show-contacts/"+contact.getcId();
		
	}
	
	//open update form handler
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId, Model model) {
		
		model.addAttribute("title", "Update Contact");
		
		Contact contact = this.contactRepository.findById(cId).get();
		
		model.addAttribute("contact", contact);
		
		return "normal/update_form";
		
	}
	
	//update contact handler
	@RequestMapping(value="/process-update", method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file, Model m, HttpSession session, Principal principal) {
		
		try {
			//old contact details
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			
			//image...
			if(!file.isEmpty()) {
				//delete old photo
				
				
				//update new photo
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
			}else {
				contact.setImage(oldContactDetail.getImage());
			}
			
			User user=this.userRepository.getUserByUserName(principal.getName());
			
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your Contact is updated..", "alert-success"));
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		
		System.out.println("CONTACT NAME "+contact.getName());
		System.out.println("CONTACT ID "+contact.getcId());
		
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	//your profile handler
	
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title", "User Profile");
		return "normal/profile";	
	}
	
	

}
