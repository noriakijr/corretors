package com.noriakijr.corretors.controller.mb;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import com.noriakijr.corretors.controller.facade.impl.Facade;
import com.noriakijr.corretors.model.IEntity;
import com.noriakijr.corretors.model.Usuario;
import com.noriakijr.corretors.utils.CriptAES;

@ManagedBean
@SessionScoped
public class UsuarioBean implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Facade facade;
	private Usuario usuario;
    private boolean loggedIn;
 
    @PostConstruct
    public void init() {
    	
    }
    
    /**
     * Login operation.
     *
     * @return String page
     */
    public String doLogin() {
    	facade = new Facade();
        // Pegar users do banco de dados
        List<IEntity> users = facade.find(new Usuario());
        for (IEntity entity : users) {
        	Usuario user = (Usuario) entity;
            if (user.getUsername().equals(usuario.getUsername())) {                          // encontrou o usuario
                // Encriptar senha
                String key = "140b41b22a29bex4061bda81b6747e14";
                String iv = "1234567890123456";
                CriptAES cript = new CriptAES(key, iv);
                try {
                    usuario.setPassword(cript.encriptar(usuario.getPassword()));
 
                    if (user.getPassword().equals(usuario.getPassword())) {                  // verifcar a senha
                        loggedIn = true;
//                        profile = user.getPerfil();
                        return "/pages/home.xhtml?faces-redirect=true";				         // redirecionar para o sistema
                    }
                } catch (Exception ex) {
                    Logger.getLogger(UsuarioBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
 
        // Set login ERROR
        FacesMessage msg = new FacesMessage(null, "Usuário e senha incorretos!");
        msg.setSeverity(FacesMessage.SEVERITY_ERROR);
        FacesContext.getCurrentInstance().addMessage(null, msg);
 
        // To to login page
        return "/pages/login.xhtml";
 
    }

    /**
     * Logout operation.
     *
     * @return String page
     */
    public String doLogout() {
        // Set the paremeter indicating that user is logged in to false
        loggedIn = false;
        usuario = null;
        // Set logout message
        FacesMessage msg = new FacesMessage(null, "Logout efetuado com sucesso!");
        msg.setSeverity(FacesMessage.SEVERITY_INFO);
        FacesContext.getCurrentInstance().addMessage(null, msg);
 
        // Go to login page
        return "/pages/login.xhtml";
    }

    /* GETTERS AND SETTERS */

    public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}
    
	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

}
