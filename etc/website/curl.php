<?php

class Curl
{
    private $curl;
    
    function __construct ($url = NULL)
    {
        $curl = curl_init ($url);
        
        if ($curl === false)
            throw new Exception ('Failed to create cURL handle');
        
        $this->curl = $curl;
        
        $this->returntransfer = true;
    }
    
    function __destruct ()
    {
        $this->close ();
    }
    
    function __set ($property, $value)
    {
        $property = 'CURLOPT_' . strtoupper ($property);
        
        if (constant ($property) === NULL)
            $property = preg_replace_callback ('#[A-Z]#', create_function ('$match', 'return "_" . strtolower($match[0]);'), $property);
        
        if (constant ($property) === NULL)
            throw new Exception ("Property doesn't exist: " . $property);
        
        $ok = curl_setopt ($this->curl, constant ($property), $value);
        
        if ($ok === false)
            throw $this->getException ();
    }
    
    public function close ()
    {
        @curl_close ($this->curl);
    }
    
    public function copy ()
    {
        return curl_copy_handle ($this->curl);
    }
    
    public function exec ()
    {
        $content = curl_exec ($this->curl);
        
        if ($content === false)
            throw $this->getException ();
        
        return new CurlResult ($this->curl, $content);
    }
    
    private function getException ()
    {
        $errno = curl_errno ($this->curl);
        $error = curl_error ($this->curl);
        
        $message = '[' . $errno . '] ' . $error;
        
        return new Exception ($message);
    }
    
    public static function version ()
    {
        return new CurlVersion ();
    }
}

class CurlResult
{
    private $curl;
    private $content;
    private $data;
    
    function __construct ($curl, $content)
    {
        $this->curl = $curl;
        $this->content = $content;
        $this->data = curl_getinfo ($curl);
    }
    
    function __destruct ()
    {
        @curl_close ($this->curl);
    }
    
    function __toString ()
    {
        return $this->content;
    }
    
    function __get ($property)
    {
        if ($property == 'content')
            return $this->content;
        
        if (isset ($this->data[$property]))
            return $this->data[$property];
        
        $snake = preg_replace_callback ('#[A-Z]#', create_function ('$match', 'return "_" . strtolower($match[0]);'), $property);
        
        if (isset ($this->data[$snake]))
            return $this->data[$snake];
        
        throw new Exception ("Property doesn't exist: " . $property);
    }
}

class CurlVersion
{
    private $data;
    
    function __construct ()
    {
        $this->data = curl_version ();
    }
    
    function __get ($property)
    {
        if (isset ($this->data[$property]))
            return $this->data[$property];
        
        $snake = preg_replace_callback ('#[A-Z]#', create_function ('$match', 'return "_" . strtolower($match[0]);'), $property);
        
        if (isset ($this->data[$snake]))
            return $this->data[$snake];
        
        throw new Exception ("Property doesn't exist: " . $property);
    }
}
?>